package fr.varyon.mixinagent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Pose le PlayerRef courant dans un ThreadLocal statique VARYON$CURRENT_PLAYER_REF
 * injecté directement dans InventoryPacketHandler (voir raison ci-dessous), au
 * début des lambdas qui traitent MoveItemStack (lambda$handle$5) et
 * SmartMoveItemStack (lambda$handle$6), et le retire avant chaque RETURN
 * existant de ces méthodes. Ces lambdas s'exécutent sur le thread du monde
 * (world.execute), le même thread sur lequel Item#getMaxStack() sera ensuite
 * appelé pendant le traitement du paquet.
 *
 * Le ThreadLocal est un champ statique de InventoryPacketHandler (classe du
 * serveur, chargée par TransformingClassLoader) plutôt qu'une classe de notre
 * jar plugin : Item et InventoryPacketHandler sont chargées par le même
 * TransformingClassLoader et peuvent se référencer librement, mais ce
 * classloader ne voit pas notre jar (early-plugin chargé par un
 * URLClassLoader séparé) — toute référence croisée vers fr.varyon.mixinagent.*
 * depuis le bytecode injecté provoquerait un NoClassDefFoundError au runtime.
 */
final class InventoryHandlerContextInjector {

    private static final String HANDLER = "com/hypixel/hytale/server/core/io/handlers/game/InventoryPacketHandler";
    private static final String PLAYER_REF = "com/hypixel/hytale/server/core/universe/PlayerRef";
    private static final String REF = "com/hypixel/hytale/component/Ref";
    private static final String STORE = "com/hypixel/hytale/component/Store";
    private static final String COMPONENT_TYPE = "com/hypixel/hytale/component/ComponentType";
    private static final String COMPONENT = "com/hypixel/hytale/component/Component";
    private static final String THREAD_LOCAL = "java/lang/ThreadLocal";

    static final String CONTEXT_FIELD_NAME = "varyon$currentPlayerRef";
    private static final String CONTEXT_FIELD_DESC = "Ljava/lang/ThreadLocal;";

    // lambda$handle$5(Ref, MoveItemStack, Store, PlayerRef) -> PlayerRef est le paramètre local #3
    private static final String MOVE_LAMBDA = "lambda$handle$5";
    private static final int MOVE_LAMBDA_PLAYER_REF_SLOT = 3;

    // lambda$handle$6(Store, Ref, SmartMoveItemStack) -> pas de PlayerRef capturé,
    // à résoudre via store.getComponent(ref, PlayerRef.getComponentType())
    private static final String SMART_MOVE_LAMBDA = "lambda$handle$6";
    private static final int SMART_MOVE_LAMBDA_STORE_SLOT = 0;
    private static final int SMART_MOVE_LAMBDA_REF_SLOT = 1;

    // lambda$handle$8(Store, Ref, InventoryAction) -> Sort / QuickStack / TakeAll / PutAll,
    // pas de PlayerRef capturé, mêmes slots que lambda$handle$6.
    private static final String INVENTORY_ACTION_LAMBDA = "lambda$handle$8";
    private static final int INVENTORY_ACTION_LAMBDA_STORE_SLOT = 0;
    private static final int INVENTORY_ACTION_LAMBDA_REF_SLOT = 1;

    // lambda$handle$0(Store, Ref, SetCreativeItem) -> give créatif (clic direct dans l'inventaire créatif)
    private static final String SET_CREATIVE_ITEM_LAMBDA = "lambda$handle$0";
    private static final int SET_CREATIVE_ITEM_LAMBDA_STORE_SLOT = 0;
    private static final int SET_CREATIVE_ITEM_LAMBDA_REF_SLOT = 1;

    // lambda$handle$3(Store, Ref, SmartGiveCreativeItem) -> give créatif (double-clic / smart give)
    private static final String SMART_GIVE_CREATIVE_ITEM_LAMBDA = "lambda$handle$3";
    private static final int SMART_GIVE_CREATIVE_ITEM_LAMBDA_STORE_SLOT = 0;
    private static final int SMART_GIVE_CREATIVE_ITEM_LAMBDA_REF_SLOT = 1;

    private static final String MARKER_NAME = "varyon$contextAware";

    private InventoryHandlerContextInjector() {
    }

    static byte[] inject(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.EXPAND_FRAMES);

        for (MethodNode m : cn.methods) {
            if (MARKER_NAME.equals(m.name)) {
                return classBytes;
            }
        }

        addContextField(cn);

        boolean patchedMove = false;
        boolean patchedSmartMove = false;
        boolean patchedInventoryAction = false;
        boolean patchedSetCreativeItem = false;
        boolean patchedSmartGiveCreativeItem = false;

        for (MethodNode m : cn.methods) {
            if (MOVE_LAMBDA.equals(m.name)) {
                setAtStart(m, loadPlayerRefFromLocal(MOVE_LAMBDA_PLAYER_REF_SLOT));
                clearBeforeEachReturn(m);
                patchedMove = true;
            } else if (SMART_MOVE_LAMBDA.equals(m.name)) {
                setAtStart(m, loadPlayerRefFromEcsComponent(SMART_MOVE_LAMBDA_STORE_SLOT, SMART_MOVE_LAMBDA_REF_SLOT));
                clearBeforeEachReturn(m);
                patchedSmartMove = true;
            } else if (INVENTORY_ACTION_LAMBDA.equals(m.name)) {
                setAtStart(m, loadPlayerRefFromEcsComponent(
                        INVENTORY_ACTION_LAMBDA_STORE_SLOT, INVENTORY_ACTION_LAMBDA_REF_SLOT));
                clearBeforeEachReturn(m);
                patchedInventoryAction = true;
            } else if (SET_CREATIVE_ITEM_LAMBDA.equals(m.name)) {
                setAtStart(m, loadPlayerRefFromEcsComponent(
                        SET_CREATIVE_ITEM_LAMBDA_STORE_SLOT, SET_CREATIVE_ITEM_LAMBDA_REF_SLOT));
                clearBeforeEachReturn(m);
                patchedSetCreativeItem = true;
            } else if (SMART_GIVE_CREATIVE_ITEM_LAMBDA.equals(m.name)) {
                setAtStart(m, loadPlayerRefFromEcsComponent(
                        SMART_GIVE_CREATIVE_ITEM_LAMBDA_STORE_SLOT, SMART_GIVE_CREATIVE_ITEM_LAMBDA_REF_SLOT));
                clearBeforeEachReturn(m);
                patchedSmartGiveCreativeItem = true;
            }
        }

        if (!patchedMove) {
            throw new IllegalStateException(MOVE_LAMBDA + " not found (Hytale internals changed) — hook not installed");
        }
        if (!patchedSmartMove) {
            throw new IllegalStateException(SMART_MOVE_LAMBDA + " not found (Hytale internals changed) — hook not installed");
        }
        if (!patchedInventoryAction) {
            throw new IllegalStateException(
                    INVENTORY_ACTION_LAMBDA + " not found (Hytale internals changed) — hook not installed");
        }
        if (!patchedSetCreativeItem) {
            throw new IllegalStateException(
                    SET_CREATIVE_ITEM_LAMBDA + " not found (Hytale internals changed) — hook not installed");
        }
        if (!patchedSmartGiveCreativeItem) {
            throw new IllegalStateException(
                    SMART_GIVE_CREATIVE_ITEM_LAMBDA + " not found (Hytale internals changed) — hook not installed");
        }

        cn.methods.add(buildMarker());
        addStaticInitializer(cn);

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void addContextField(ClassNode cn) {
        FieldNode field = new FieldNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                CONTEXT_FIELD_NAME, CONTEXT_FIELD_DESC, null, null);
        cn.fields.add(field);
    }

    /** Ajoute `VARYON_FIELD = new ThreadLocal();` dans <clinit>, en tête (ou crée <clinit> si absent). */
    private static void addStaticInitializer(ClassNode cn) {
        MethodNode clinit = null;
        for (MethodNode m : cn.methods) {
            if ("<clinit>".equals(m.name)) {
                clinit = m;
                break;
            }
        }
        InsnList init = new InsnList();
        init.add(new TypeInsnNode(Opcodes.NEW, THREAD_LOCAL));
        init.add(new InsnNode(Opcodes.DUP));
        init.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, THREAD_LOCAL, "<init>", "()V", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, HANDLER, CONTEXT_FIELD_NAME, CONTEXT_FIELD_DESC));

        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            init.add(new InsnNode(Opcodes.RETURN));
            clinit.instructions.add(init);
            cn.methods.add(clinit);
        } else {
            AbstractInsnNode first = clinit.instructions.getFirst();
            if (first != null) {
                clinit.instructions.insertBefore(first, init);
            } else {
                init.add(new InsnNode(Opcodes.RETURN));
                clinit.instructions.add(init);
            }
        }
    }

    /** Charge le PlayerRef déjà présent en variable locale, puis appelle FIELD.set(playerRef). */
    private static InsnList loadPlayerRefFromLocal(int slot) {
        InsnList il = new InsnList();
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, HANDLER, CONTEXT_FIELD_NAME, CONTEXT_FIELD_DESC));
        il.add(new VarInsnNode(Opcodes.ALOAD, slot));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, THREAD_LOCAL, "set", "(Ljava/lang/Object;)V", false));
        return il;
    }

    /** Résout le PlayerRef via le composant ECS, puis appelle FIELD.set(playerRef). */
    private static InsnList loadPlayerRefFromEcsComponent(int storeSlot, int refSlot) {
        InsnList il = new InsnList();
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, HANDLER, CONTEXT_FIELD_NAME, CONTEXT_FIELD_DESC));
        il.add(new VarInsnNode(Opcodes.ALOAD, storeSlot));
        il.add(new VarInsnNode(Opcodes.ALOAD, refSlot));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PLAYER_REF, "getComponentType",
                "()L" + COMPONENT_TYPE + ";", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STORE, "getComponent",
                "(L" + REF + ";L" + COMPONENT_TYPE + ";)L" + COMPONENT + ";", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, THREAD_LOCAL, "set", "(Ljava/lang/Object;)V", false));
        return il;
    }

    private static void setAtStart(MethodNode m, InsnList setupCode) {
        AbstractInsnNode first = m.instructions.getFirst();
        if (first != null) {
            m.instructions.insertBefore(first, setupCode);
        } else {
            m.instructions.add(setupCode);
        }
    }

    private static void clearBeforeEachReturn(MethodNode m) {
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            int op = insn.getOpcode();
            if (op == Opcodes.RETURN || op == Opcodes.ARETURN || op == Opcodes.IRETURN
                    || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN) {
                InsnList clearCall = new InsnList();
                clearCall.add(new FieldInsnNode(Opcodes.GETSTATIC, HANDLER, CONTEXT_FIELD_NAME, CONTEXT_FIELD_DESC));
                clearCall.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, THREAD_LOCAL, "remove", "()V", false));
                m.instructions.insertBefore(insn, clearCall);
            }
        }
    }

    private static MethodNode buildMarker() {
        MethodNode mn = new MethodNode(Opcodes.ASM9,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                MARKER_NAME, "()V", null, null);
        InsnList il = mn.instructions;
        il.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 0;
        mn.maxLocals = 0;
        return mn;
    }
}
