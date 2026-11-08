package mihon.extension.runtime.jvm.conversion

import com.googlecode.d2j.DexConstants
import com.googlecode.d2j.Method
import com.googlecode.d2j.Proto
import com.googlecode.d2j.node.DexClassNode
import com.googlecode.d2j.node.DexCodeNode
import com.googlecode.d2j.node.DexFileNode
import com.googlecode.d2j.node.DexMethodNode
import com.googlecode.d2j.node.insn.MethodStmtNode
import com.googlecode.d2j.node.insn.TypeStmtNode
import com.googlecode.d2j.reader.Op

/**
 * Puts back, in an apk's dex code before dex2jar converts it, the constructors R8 removed.
 *
 * R8 drops a constructor that only calls its superclass's, and has whoever called it call the superclass's directly:
 * `new-instance Sub` followed by `invoke-direct Super.<init>`, or a constructor calling its grandparent's. ART runs
 * that; the JVM requires a new object's class's own constructor and a constructor's direct superclass's, and dex2jar
 * makes it worse by typing the new object as the superclass, which ends in an `InstantiationError` on an abstract
 * class or a `VerifyError` on a field of the subclass's type. Keiyoushi's builds have this shape throughout: filters
 * created as `Filter.Text`, interceptors as `Object`, maps as `LinkedHashMap`.
 *
 * Each such call is pointed at the constructor the JVM expects, which is added, with the same parameters, calling the
 * next one up until the one R8 called. A class created this way that R8 left abstract without an abstract method is made
 * concrete, as ART instantiates it anyway.
 */
internal object RemovedConstructorFix {

    fun apply(dex: DexFileNode) {
        val classes = dex.clzs.associateBy { it.className }
        val fix = Fix(classes)
        // A class without methods has none listed rather than an empty list
        val methods = dex.clzs.flatMap { owner -> owner.methods.orEmpty().map { owner to it } }
        for ((owner, method) in methods) {
            method.codeNode?.let { fix.method(owner, method, it) }
        }
        fix.instantiated.mapNotNull(classes::get)
            .filter { it.access and DexConstants.ACC_ABSTRACT != 0 && it.access and DexConstants.ACC_INTERFACE == 0 }
            .filter { node -> node.methods.orEmpty().none { it.access and DexConstants.ACC_ABSTRACT != 0 } }
            .forEach { it.access = it.access and DexConstants.ACC_ABSTRACT.inv() }
    }

    private class Fix(private val classes: Map<String, DexClassNode>) {
        val instantiated = HashSet<String>()

        fun method(owner: DexClassNode, method: DexMethodNode, code: DexCodeNode) {
            val isConstructor = method.method.name == CONSTRUCTOR
            val thisRegister = if (method.access and DexConstants.ACC_STATIC == 0) {
                code.totalRegister - registerWords(method.method.proto) - 1
            } else {
                -1
            }
            // The class each register's object was created as, until its constructor is called
            val created = HashMap<Int, String>()
            code.stmts.forEachIndexed { index, stmt ->
                if (stmt is TypeStmtNode && stmt.op == Op.NEW_INSTANCE) {
                    created[stmt.a] = stmt.type
                    return@forEachIndexed
                }
                if (stmt !is MethodStmtNode || stmt.method.name != CONSTRUCTOR) return@forEachIndexed
                if (stmt.op != Op.INVOKE_DIRECT && stmt.op != Op.INVOKE_DIRECT_RANGE) return@forEachIndexed
                val receiver = stmt.args.firstOrNull() ?: return@forEachIndexed
                val target = stmt.method.owner
                val newClass = created.remove(receiver)
                val expected = when {
                    newClass != null -> newClass
                    // Delegating to another of its own constructors is as valid on the JVM
                    isConstructor && receiver == thisRegister && target != owner.className -> owner.superClass
                    else -> null
                } ?: return@forEachIndexed
                if (expected == target || !ensureConstructor(expected, stmt.method.proto, target)) return@forEachIndexed
                code.stmts[index] = MethodStmtNode(stmt.op, stmt.args, Method(expected, CONSTRUCTOR, stmt.method.proto))
                    .also { it.index = stmt.index }
                if (newClass != null) instantiated += newClass
            }
        }

        /**
         * Gives [className] a constructor taking [proto] that ends up calling [target]'s, adding one to each class up
         * the way that lacks it. `false` when the way leaves the extension's own classes before [target].
         */
        private fun ensureConstructor(className: String, proto: Proto, target: String, depth: Int = 0): Boolean {
            if (className == target) return true
            val node = classes[className] ?: return false
            val superClass = node.superClass ?: return false
            if (node.methods.orEmpty().any { it.method.name == CONSTRUCTOR && it.method.proto == proto }) return true
            if (depth > MAX_DEPTH || !ensureConstructor(superClass, proto, target, depth + 1)) return false
            node.methods = node.methods.orEmpty() + superCallingConstructor(className, superClass, proto)
            return true
        }

        private fun superCallingConstructor(className: String, superClass: String, proto: Proto): DexMethodNode {
            // The receiver and the parameters are the last registers, so they're all of them here
            val registers = registerWords(proto) + 1
            val code = DexCodeNode()
            code.visitRegister(registers)
            code.visitMethodStmt(
                Op.INVOKE_DIRECT_RANGE,
                IntArray(registers) { it },
                Method(superClass, CONSTRUCTOR, proto),
            )
            code.visitStmt0R(Op.RETURN_VOID)
            return DexMethodNode(
                DexConstants.ACC_PUBLIC or DexConstants.ACC_CONSTRUCTOR,
                Method(className, CONSTRUCTOR, proto),
            ).also { it.codeNode = code }
        }
    }

    /** The registers [proto]'s parameters take: two for a `long` or `double`, one for anything else. */
    private fun registerWords(proto: Proto): Int = proto.parameterTypes.sumOf { if (it == "J" || it == "D") 2 else 1 }

    private const val CONSTRUCTOR = "<init>"
    private const val MAX_DEPTH = 32
}
