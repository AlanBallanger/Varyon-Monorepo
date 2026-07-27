package fr.varyon.mixinagent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Remplace intégralement le corps de Item#getMaxStack() : pour l'item ciblé
 * (Wood_Ash_Trunk), si le PlayerRef courant (lu depuis le ThreadLocal statique
 * injecté dans InventoryPacketHandler, cf. InventoryHandlerContextInjector) a
 * la permission dédiée, retourne 200 au lieu de this.maxStack (100). Pour
 * tout autre item, comportement inchangé (return this.maxStack).
 *
 * Le bytecode injecté référence uniquement des classes du serveur (Item,
 * InventoryPacketHandler, PlayerRef, ThreadLocal) — jamais notre jar plugin,
 * car Item est chargée par TransformingClassLoader qui ne voit pas les
 * classes de earlyplugins/ (chargées par un URLClassLoader séparé).
 *
 * Équivalent Java :
 *   public int getMaxStack() {
 *       if (TARGET_ITEM_ID.equals(this.id)) {
 *           PlayerRef p = (PlayerRef) InventoryPacketHandler.varyon$currentPlayerRef.get();
 *           if (p != null && p.hasPermission(PERMISSION_NODE, false)) {
 *               return BOOSTED_MAX_STACK;
 *           }
 *       }
 *       return this.maxStack;
 *   }
 */
final class MaxStackPermissionInjector {

    private static final String ITEM = "com/hypixel/hytale/server/core/asset/type/item/config/Item";
    private static final String HANDLER = "com/hypixel/hytale/server/core/io/handlers/game/InventoryPacketHandler";
    private static final String PLAYER_REF = "com/hypixel/hytale/server/core/universe/PlayerRef";
    private static final String THREAD_LOCAL = "java/lang/ThreadLocal";
    private static final String GET_MAX_STACK_NAME = "getMaxStack";
    private static final String GET_MAX_STACK_DESC = "()I";
    private static final String MARKER_NAME = "varyon$permissionAware";

    private static final String TARGET_ITEM_ID = "Wood_Ash_Trunk";
    private static final String PERMISSION_NODE = "varyon.stack.wood_ash_trunk.200";
    private static final int BOOSTED_MAX_STACK = 200;

    private MaxStackPermissionInjector() {
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

        MethodNode target = null;
        for (MethodNode m : cn.methods) {
            if (GET_MAX_STACK_NAME.equals(m.name) && GET_MAX_STACK_DESC.equals(m.desc)) {
                target = m;
                break;
            }
        }
        if (target == null) {
            throw new IllegalStateException(
                    "Item.getMaxStack()I not found (Hytale internals changed) — hook not installed");
        }

        target.instructions.clear();
        target.tryCatchBlocks.clear();
        buildBody(target.instructions);
        target.maxStack = 3;
        target.maxLocals = 2;

        cn.methods.add(buildMarker());

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void buildBody(InsnList il) {
        LabelNode fallback = new LabelNode();
        LabelNode checkPermission = new LabelNode();

        // if (!TARGET_ITEM_ID.equals(this.id)) goto fallback;
        il.add(new LdcInsnNode(TARGET_ITEM_ID));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, ITEM, "id", "Ljava/lang/String;"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals",
                "(Ljava/lang/Object;)Z", false));
        il.add(new JumpInsnNode(Opcodes.IFEQ, fallback));

        // PlayerRef p = (PlayerRef) InventoryPacketHandler.VARYON_FIELD.get();
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, HANDLER,
                InventoryHandlerContextInjector.CONTEXT_FIELD_NAME, "Ljava/lang/ThreadLocal;"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, THREAD_LOCAL, "get", "()Ljava/lang/Object;", false));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, PLAYER_REF));
        il.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // if (p == null) goto fallback;
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new JumpInsnNode(Opcodes.IFNULL, fallback));

        // if (!p.hasPermission(PERMISSION_NODE, false)) goto fallback;
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new LdcInsnNode(PERMISSION_NODE));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER_REF, "hasPermission",
                "(Ljava/lang/String;Z)Z", false));
        il.add(new JumpInsnNode(Opcodes.IFEQ, fallback));

        // return BOOSTED_MAX_STACK;
        il.add(new IntInsnNode(Opcodes.SIPUSH, BOOSTED_MAX_STACK));
        il.add(new InsnNode(Opcodes.IRETURN));

        // fallback: return this.maxStack;
        il.add(fallback);
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, ITEM, "maxStack", "I"));
        il.add(new InsnNode(Opcodes.IRETURN));
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
