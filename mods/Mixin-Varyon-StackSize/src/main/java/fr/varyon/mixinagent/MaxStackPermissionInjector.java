package fr.varyon.mixinagent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Remplace intégralement le corps de Item#getMaxStack() : pour tout item présent
 * dans la table stackable_items.txt (bundlée dans notre jar, lue au build/transform
 * time — pas au runtime serveur), calcule un multiplicateur de stack en fonction
 * des permissions du PlayerRef courant (lu depuis le ThreadLocal statique injecté
 * dans InventoryPacketHandler, cf. InventoryHandlerContextInjector) :
 *
 *   varyon.stack.all.tier{1,2,3}      -> s'applique à tous les items de la table
 *   varyon.stack.<categorie>.tier{1,2,3} -> s'applique aux items de cette catégorie
 *
 * tier1 = x1.25, tier2 = x1.5, tier3 = x2.0 (le meilleur multiplicateur parmi
 * "all" et les catégories de l'item est retenu). Le résultat est toujours arrondi
 * vers le haut (Math.ceil) : 10 -> 13 en tier1, 25 -> 32 en tier1, etc.
 *
 * En plus de LuckPerms (hasPermission), chaque catégorie (hors "all") est aussi
 * vérifiée contre une table d'overrides par joueur — varyon$playerTierOverrides,
 * une Map<String,String> statique (clé "uuid|categorie" -> tier "1"/"2"/"3")
 * injectée vide dans <clinit> et peuplée à chaud par le plugin classique
 * Varyon-StackTiers via réflexion (pont cross-classloader, cf. son propre
 * OverrideBridge). Le multiplicateur retenu pour une catégorie donnée est le
 * meilleur des deux sources (permissions OU overrides) — "all" reste
 * permission-only, ce n'est pas un scope assignable via cet override.
 *
 * La table id -> (baseStack, catégories) est entièrement résolue à la génération
 * du bytecode (voir StackTable) et embarquée sous forme d'une unique chaîne
 * constante + un HashMap construit dans <clinit>, car le bytecode injecté dans
 * Item (classe serveur, chargée par TransformingClassLoader) ne peut référencer
 * aucune classe de notre jar (chargé par un URLClassLoader séparé) — seules les
 * classes JDK (String, HashMap, ConcurrentHashMap...) sont utilisables. Pour la
 * même raison, varyon$playerTierOverrides est déclarée en Map (pas
 * ConcurrentHashMap) pour rester cohérente avec varyon$stackTable et n'utiliser
 * que des appels INVOKEINTERFACE déjà en place.
 */
final class MaxStackPermissionInjector {

    private static final String ITEM = "com/hypixel/hytale/server/core/asset/type/item/config/Item";
    private static final String HANDLER = "com/hypixel/hytale/server/core/io/handlers/game/InventoryPacketHandler";
    private static final String PLAYER_REF = "com/hypixel/hytale/server/core/universe/PlayerRef";
    private static final String THREAD_LOCAL = "java/lang/ThreadLocal";
    private static final String STRING = "java/lang/String";
    private static final String HASH_MAP = "java/util/HashMap";
    private static final String CONCURRENT_HASH_MAP = "java/util/concurrent/ConcurrentHashMap";
    private static final String MAP = "java/util/Map";
    private static final String MATH = "java/lang/Math";

    private static final String GET_MAX_STACK_NAME = "getMaxStack";
    private static final String GET_MAX_STACK_DESC = "()I";
    private static final String MARKER_NAME = "varyon$permissionAware";

    private static final String TABLE_FIELD_NAME = "varyon$stackTable";
    private static final String TABLE_FIELD_DESC = "L" + MAP + ";";
    private static final String TABLE_INIT_METHOD_NAME = "varyon$initStackTable";

    private static final String OVERRIDES_FIELD_NAME = "varyon$playerTierOverrides";
    private static final String OVERRIDES_FIELD_DESC = "L" + MAP + ";";
    private static final String OVERRIDES_INIT_METHOD_NAME = "varyon$initTierOverrides";

    private static final String PERMISSION_PREFIX = "varyon.stack.";
    private static final String SCOPE_ALL = "all";
    private static final String[] TIER_SUFFIXES = {".tier3", ".tier2", ".tier1"};
    private static final double[] TIER_MULTIPLIERS = {2.0, 1.5, 1.25};

    private static final String RESOURCE_NAME = "/fr/varyon/mixinagent/stackable_items.txt";

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

        StackTable table = StackTable.load();

        addTableField(cn);
        addTableInitializer(cn, table.encoded());
        addOverridesField(cn);
        addOverridesInitializer(cn);
        addStaticInitCall(cn);

        target.instructions.clear();
        target.tryCatchBlocks.clear();
        buildBody(target.instructions);
        target.maxStack = 6;
        target.maxLocals = 10;

        cn.methods.add(buildMarker());

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void addTableField(ClassNode cn) {
        FieldNode field = new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_FINAL,
                TABLE_FIELD_NAME, TABLE_FIELD_DESC, null, null);
        cn.fields.add(field);
    }

    /**
     * Champ pont vers Varyon-StackTiers (plugin classique séparé) : une
     * ConcurrentHashMap<String,String> vide au démarrage, peuplée à chaud par ce
     * plugin via réflexion (Class.forName + getDeclaredField + setAccessible).
     * Pas ACC_FINAL : l'identité de la map est fixée une seule fois dans <clinit>
     * (comme varyon$stackTable), mais la garder non-final coûte rien et laisse la
     * porte ouverte à une réassignation future sans casser la vérification bytecode.
     */
    private static void addOverridesField(ClassNode cn) {
        FieldNode field = new FieldNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                OVERRIDES_FIELD_NAME, OVERRIDES_FIELD_DESC, null, null);
        cn.fields.add(field);
    }

    /** varyon$initTierOverrides(): Map -- retourne simplement new ConcurrentHashMap() (vide). */
    private static void addOverridesInitializer(ClassNode cn) {
        MethodNode mn = new MethodNode(Opcodes.ASM9,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                OVERRIDES_INIT_METHOD_NAME, "()L" + MAP + ";", null, null);
        InsnList il = mn.instructions;
        il.add(new TypeInsnNode(Opcodes.NEW, CONCURRENT_HASH_MAP));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, CONCURRENT_HASH_MAP, "<init>", "()V", false));
        il.add(new InsnNode(Opcodes.ARETURN));
        mn.maxStack = 2;
        mn.maxLocals = 0;
        cn.methods.add(mn);
    }

    /**
     * varyon$initStackTable(String encoded): Map
     *   entries separated by ';', each "id|baseStack|cat1,cat2"
     *   parses into a HashMap<String, String[2]> { id -> [baseStackStr, catsCsv] }
     */
    private static void addTableInitializer(ClassNode cn, String encoded) {
        MethodNode mn = new MethodNode(Opcodes.ASM9,
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                TABLE_INIT_METHOD_NAME, "()L" + MAP + ";", null, null);
        InsnList il = mn.instructions;

        // Map map = new HashMap();
        il.add(new TypeInsnNode(Opcodes.NEW, HASH_MAP));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, HASH_MAP, "<init>", "()V", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // String[] entries = (CHUNK_0 + CHUNK_1 + ...).split(";");
        // Une constante String (CONSTANT_Utf8) est limitée à 65535 octets — la table
        // dépasse cette limite, donc elle est découpée en morceaux (sur des frontières
        // d'entrée, jamais au milieu d'un "id|stack|cats") concaténés via StringBuilder.
        emitConcatenatedChunks(il, chunkEncoded(encoded));
        il.add(new LdcInsnNode(";"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STRING, "split",
                "(Ljava/lang/String;)[Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 2));

        // for (int i = 0; i < entries.length; i++)
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ISTORE, 3));

        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();
        il.add(loopStart);
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new InsnNode(Opcodes.ARRAYLENGTH));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));

        // String[] parts = entries[i].split("\\|");
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new LdcInsnNode("\\|"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STRING, "split",
                "(Ljava/lang/String;)[Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 4));

        // map.put(parts[0], new String[]{ parts[1], parts[2] });
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.AALOAD));

        il.add(new InsnNode(Opcodes.ICONST_2));
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, STRING));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new InsnNode(Opcodes.AASTORE));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new InsnNode(Opcodes.ICONST_2));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new InsnNode(Opcodes.AASTORE));

        il.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, MAP, "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true));
        il.add(new InsnNode(Opcodes.POP));

        // i++
        il.add(new IincInsnNode(3, 1));
        il.add(new JumpInsnNode(Opcodes.GOTO, loopStart));

        il.add(loopEnd);
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new InsnNode(Opcodes.ARETURN));

        mn.maxStack = 6;
        mn.maxLocals = 5;
        cn.methods.add(mn);
    }

    /** Limite CONSTANT_Utf8 (65535 octets) moins une marge de sécurité pour l'encodage UTF-8. */
    private static final int MAX_CHUNK_CHARS = 60000;

    /** Découpe `encoded` en morceaux ≤ MAX_CHUNK_CHARS, uniquement sur des frontières d'entrée (';'). */
    private static java.util.List<String> chunkEncoded(String encoded) {
        java.util.List<String> chunks = new java.util.ArrayList<>();
        String[] parts = encoded.split(";");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            int extra = (current.length() > 0 ? 1 : 0) + part.length();
            if (current.length() + extra > MAX_CHUNK_CHARS && current.length() > 0) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(';');
            }
            current.append(part);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /** Empile chunk0 + chunk1 + ... via StringBuilder (chaque chunk est une LDC distincte). */
    private static void emitConcatenatedChunks(InsnList il, java.util.List<String> chunks) {
        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        for (String chunk : chunks) {
            il.add(new LdcInsnNode(chunk));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        }
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
    }

    /** Ajoute `FIELD = varyon$initStackTable();` en tête de <clinit> (le crée si absent). */
    private static void addStaticInitCall(ClassNode cn) {
        MethodNode clinit = null;
        for (MethodNode m : cn.methods) {
            if ("<clinit>".equals(m.name)) {
                clinit = m;
                break;
            }
        }
        InsnList init = new InsnList();
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ITEM, TABLE_INIT_METHOD_NAME,
                "()L" + MAP + ";", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, ITEM, TABLE_FIELD_NAME, TABLE_FIELD_DESC));
        init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ITEM, OVERRIDES_INIT_METHOD_NAME,
                "()L" + MAP + ";", false));
        init.add(new FieldInsnNode(Opcodes.PUTSTATIC, ITEM, OVERRIDES_FIELD_NAME, OVERRIDES_FIELD_DESC));

        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            init.add(new InsnNode(Opcodes.RETURN));
            clinit.instructions.add(init);
            cn.methods.add(clinit);
        } else {
            org.objectweb.asm.tree.AbstractInsnNode first = clinit.instructions.getFirst();
            if (first != null) {
                clinit.instructions.insertBefore(first, init);
            } else {
                init.add(new InsnNode(Opcodes.RETURN));
                clinit.instructions.add(init);
            }
        }
    }

    /**
     * Équivalent Java du corps généré :
     *
     * public int getMaxStack() {
     *     String[] entry = (String[]) VARYON_TABLE.get(this.id);
     *     if (entry == null) return this.maxStack;
     *     PlayerRef p = (PlayerRef) InventoryPacketHandler.varyon$currentPlayerRef.get();
     *     if (p == null) return this.maxStack;
     *     double best = varyon$bestMultiplier(p, "all");
     *     String[] cats = entry[1].split(",");
     *     for (String cat : cats) best = Math.max(best, varyon$bestMultiplier(p, cat));
     *     if (best <= 1.0) return this.maxStack;
     *     int base = Integer.parseInt(entry[0]);
     *     return (int) Math.round(base * best);
     * }
     *
     * varyon$bestMultiplier(PlayerRef p, String scope) teste, pour le scope "all"
     * uniquement, "varyon.stack." + scope + ".tier3" / ".tier2" / ".tier1" dans cet
     * ordre via hasPermission et retourne 2.0 / 1.5 / 1.25 / 1.0 (aucune permission).
     * Pour les scopes de catégorie (pas "all"), le résultat est le max entre cette
     * même chaîne hasPermission ET une lecture de varyon$playerTierOverrides.get(
     * uuidStr + "|" + scope) (tier "1"/"2"/"3" -> 1.25/1.5/2.0, absent -> 1.0).
     *
     * Slots locaux : 0=this, 1=entry(String[]), 2=p(PlayerRef), 3-4=best(double),
     * 5=cats(String[], boucle catégorie), 6=i(int, boucle catégorie),
     * 7=uuidStr(String, calculé une fois), 8=tierStr(String, scratch overrides).
     */
    private static void buildBody(InsnList il) {
        LabelNode fallback = new LabelNode();

        // String[] entry = (String[]) VARYON_TABLE.get(this.id);
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, ITEM, TABLE_FIELD_NAME, TABLE_FIELD_DESC));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, ITEM, "id", "Ljava/lang/String;"));
        il.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, MAP, "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;", true));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, "[Ljava/lang/String;"));
        il.add(new VarInsnNode(Opcodes.ASTORE, 1));

        // if (entry == null) goto fallback;
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new JumpInsnNode(Opcodes.IFNULL, fallback));

        // PlayerRef p = (PlayerRef) InventoryPacketHandler.varyon$currentPlayerRef.get();
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, HANDLER,
                InventoryHandlerContextInjector.CONTEXT_FIELD_NAME, "Ljava/lang/ThreadLocal;"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, THREAD_LOCAL, "get", "()Ljava/lang/Object;", false));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, PLAYER_REF));
        il.add(new VarInsnNode(Opcodes.ASTORE, 2));

        // if (p == null) goto fallback;
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new JumpInsnNode(Opcodes.IFNULL, fallback));

        // String uuidStr = p.getUuid().toString();
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER_REF, "getUuid", "()Ljava/util/UUID;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 7));

        // double best = bestMultiplier(p, "all"); (permission-only, "all" n'est pas overridable)
        emitBestMultiplierLiteral(il, SCOPE_ALL);
        il.add(new VarInsnNode(Opcodes.DSTORE, 3));

        // String[] cats = entry[1].split(",");
        // for (String cat : cats) best = Math.max(best, bestMultiplier(p, cat));
        buildCategoryLoop(il);

        // if (best <= 1.0) goto fallback;
        il.add(new VarInsnNode(Opcodes.DLOAD, 3));
        il.add(new InsnNode(Opcodes.DCONST_1));
        il.add(new InsnNode(Opcodes.DCMPG));
        il.add(new JumpInsnNode(Opcodes.IFLE, fallback));

        // int base = Integer.parseInt(entry[0]);
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt",
                "(Ljava/lang/String;)I", false));
        il.add(new InsnNode(Opcodes.I2D));

        // (int) Math.ceil(base * best) -- toujours arrondi vers le haut (25 * 1.25 = 31.25 -> 32)
        il.add(new VarInsnNode(Opcodes.DLOAD, 3));
        il.add(new InsnNode(Opcodes.DMUL));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MATH, "ceil", "(D)D", false));
        il.add(new InsnNode(Opcodes.D2I));
        il.add(new InsnNode(Opcodes.IRETURN));

        // fallback: return this.maxStack;
        il.add(fallback);
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETFIELD, ITEM, "maxStack", "I"));
        il.add(new InsnNode(Opcodes.IRETURN));
    }

    /**
     * for (int i = 0; i < cats.length; i++) best = Math.max(best, bestMultiplier(p, cats[i]));
     * cats = entry[1].split(",")
     * Slots : 5=cats(String[]), 6=i(int)
     */
    private static void buildCategoryLoop(InsnList il) {
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new LdcInsnNode(","));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STRING, "split",
                "(Ljava/lang/String;)[Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 5));

        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ISTORE, 6));

        LabelNode loopStart = new LabelNode();
        LabelNode loopEnd = new LabelNode();
        il.add(loopStart);
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(new VarInsnNode(Opcodes.ALOAD, 5));
        il.add(new InsnNode(Opcodes.ARRAYLENGTH));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, loopEnd));

        // best = Math.max(best, bestMultiplier(p, cats[i]));  (permissions OU overrides)
        il.add(new VarInsnNode(Opcodes.DLOAD, 3));
        emitBestFromPermissionsFromArray(il);
        emitBestFromOverrideMapFromArray(il);
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MATH, "max", "(DD)D", false));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MATH, "max", "(DD)D", false));
        il.add(new VarInsnNode(Opcodes.DSTORE, 3));

        il.add(new IincInsnNode(6, 1));
        il.add(new JumpInsnNode(Opcodes.GOTO, loopStart));
        il.add(loopEnd);
    }

    /** Empile bestFromPermissions(p, cats[i]) — scope lu dynamiquement depuis le tableau local 5 à l'index 6. */
    private static void emitBestFromPermissionsFromArray(InsnList il) {
        // Teste, dans l'ordre tier3 puis tier2 puis tier1, p.hasPermission("varyon.stack." + cats[i] + suffix, false)
        LabelNode end = new LabelNode();
        LabelNode[] next = new LabelNode[TIER_SUFFIXES.length];
        for (int t = 0; t < TIER_SUFFIXES.length; t++) {
            next[t] = new LabelNode();
            il.add(new VarInsnNode(Opcodes.ALOAD, 2));

            // "varyon.stack." + cats[i] + suffix
            il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
            il.add(new InsnNode(Opcodes.DUP));
            il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
            il.add(new LdcInsnNode(PERMISSION_PREFIX));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            il.add(new VarInsnNode(Opcodes.ALOAD, 5));
            il.add(new VarInsnNode(Opcodes.ILOAD, 6));
            il.add(new InsnNode(Opcodes.AALOAD));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            il.add(new LdcInsnNode(TIER_SUFFIXES[t]));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false));

            il.add(new InsnNode(Opcodes.ICONST_0));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER_REF, "hasPermission",
                    "(Ljava/lang/String;Z)Z", false));
            il.add(new JumpInsnNode(Opcodes.IFEQ, next[t]));

            il.add(new LdcInsnNode(TIER_MULTIPLIERS[t]));
            il.add(new JumpInsnNode(Opcodes.GOTO, end));
            il.add(next[t]);
        }
        il.add(new LdcInsnNode(1.0));
        il.add(end);
    }

    /**
     * Empile bestFromOverrideMap(cats[i]) : lit varyon$playerTierOverrides.get(
     * uuidStr + "|" + cats[i]) et convertit le premier caractère de la valeur
     * ("3"/"2"/"1") en multiplicateur ; absent -> 1.0.
     */
    private static void emitBestFromOverrideMapFromArray(InsnList il) {
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, ITEM, OVERRIDES_FIELD_NAME, OVERRIDES_FIELD_DESC));
        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        il.add(new VarInsnNode(Opcodes.ALOAD, 7)); // uuidStr
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        il.add(new LdcInsnNode("|"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        il.add(new VarInsnNode(Opcodes.ALOAD, 5)); // cats
        il.add(new VarInsnNode(Opcodes.ILOAD, 6)); // i
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                "()Ljava/lang/String;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, MAP, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, STRING));
        emitTierStringToMultiplier(il);
    }

    /** Empile bestMultiplier(p, literalScope) — scope constant connu au build time (utilisé pour "all"). */
    private static void emitBestMultiplierLiteral(InsnList il, String literalScope) {
        emitBestFromPermissionsLiteral(il, literalScope);
    }

    /** Empile bestFromPermissions(p, literalScope) — scope constant connu au build time. */
    private static void emitBestFromPermissionsLiteral(InsnList il, String literalScope) {
        LabelNode end = new LabelNode();
        LabelNode[] next = new LabelNode[TIER_SUFFIXES.length];
        for (int t = 0; t < TIER_SUFFIXES.length; t++) {
            next[t] = new LabelNode();
            il.add(new VarInsnNode(Opcodes.ALOAD, 2));
            il.add(new LdcInsnNode(PERMISSION_PREFIX + literalScope + TIER_SUFFIXES[t]));
            il.add(new InsnNode(Opcodes.ICONST_0));
            il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PLAYER_REF, "hasPermission",
                    "(Ljava/lang/String;Z)Z", false));
            il.add(new JumpInsnNode(Opcodes.IFEQ, next[t]));

            il.add(new LdcInsnNode(TIER_MULTIPLIERS[t]));
            il.add(new JumpInsnNode(Opcodes.GOTO, end));
            il.add(next[t]);
        }
        il.add(new LdcInsnNode(1.0));
        il.add(end);
    }

    /**
     * Empile le multiplicateur correspondant à la String de tier au sommet de pile
     * (consommée) : null -> 1.0, sinon premier caractère '3'->2.0, '2'->1.5,
     * '1'->1.25, autre caractère -> 1.0. Stocke la valeur dans le slot scratch 8
     * (tierStr) pour le test de nullité avant de lire charAt(0).
     */
    private static void emitTierStringToMultiplier(InsnList il) {
        il.add(new VarInsnNode(Opcodes.ASTORE, 8));

        LabelNode isNull = new LabelNode();
        LabelNode end = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ALOAD, 8));
        il.add(new JumpInsnNode(Opcodes.IFNULL, isNull));

        il.add(new VarInsnNode(Opcodes.ALOAD, 8));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STRING, "charAt", "(I)C", false));
        il.add(new VarInsnNode(Opcodes.ISTORE, 9)); // slot dédié : le slot 6 est l'index actif de la boucle catégorie

        char[] tierChars = {'3', '2', '1'};
        LabelNode[] next = new LabelNode[tierChars.length];
        for (int t = 0; t < tierChars.length; t++) {
            next[t] = new LabelNode();
            il.add(new VarInsnNode(Opcodes.ILOAD, 9));
            il.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, tierChars[t]));
            il.add(new JumpInsnNode(Opcodes.IF_ICMPNE, next[t]));
            il.add(new LdcInsnNode(TIER_MULTIPLIERS[t]));
            il.add(new JumpInsnNode(Opcodes.GOTO, end));
            il.add(next[t]);
        }
        il.add(new LdcInsnNode(1.0));
        il.add(new JumpInsnNode(Opcodes.GOTO, end));

        il.add(isNull);
        il.add(new LdcInsnNode(1.0));

        il.add(end);
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

    /** Charge la table id -> (baseStack, catégories) depuis la ressource embarquée dans notre jar. */
    private static final class StackTable {
        private final String encoded;

        private StackTable(String encoded) {
            this.encoded = encoded;
        }

        String encoded() {
            return encoded;
        }

        static StackTable load() {
            try (InputStream in = MaxStackPermissionInjector.class.getResourceAsStream(RESOURCE_NAME)) {
                if (in == null) {
                    throw new IllegalStateException("Resource not found: " + RESOURCE_NAME);
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    boolean first = true;
                    while ((line = reader.readLine()) != null) {
                        if (!first) {
                            sb.append('\n');
                        }
                        sb.append(line);
                        first = false;
                    }
                }
                return new StackTable(sb.toString().trim());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load " + RESOURCE_NAME, e);
            }
        }
    }
}
