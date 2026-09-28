package com.dazaike.ciderpatcher.core

/** Parameter order of the obfuscated `BaseContinuationImpl.create(Any?, Continuation)` override. */
enum class CreateOrder { CONT_FIRST, VALUE_FIRST }

/** Placeholder name (without braces) → smali literal, for one Cider build. */
data class SymbolMap(val values: Map<String, String>, val createOrder: CreateOrder) {
    companion object {
        /** Adds `CREATE_DESCRIPTOR`, `CREATE_COMPLETION_REG`, `CREATE_CALL_ARGS` derived from [order]. */
        fun withDerived(base: Map<String, String>, order: CreateOrder): SymbolMap {
            val cont = base["T_CONTINUATION"] ?: throw PatchException("Unfilled placeholder T_CONTINUATION in SymbolMap")
            val derived = when (order) {
                CreateOrder.CONT_FIRST -> mapOf(
                    "CREATE_DESCRIPTOR" to "(${cont}Ljava/lang/Object;)$cont",
                    "CREATE_COMPLETION_REG" to "p1",
                    "CREATE_CALL_ARGS" to "p2, p1",
                )
                CreateOrder.VALUE_FIRST -> mapOf(
                    "CREATE_DESCRIPTOR" to "(Ljava/lang/Object;$cont)$cont",
                    "CREATE_COMPLETION_REG" to "p2",
                    "CREATE_CALL_ARGS" to "p1, p2",
                )
            }
            return SymbolMap(base + derived, order)
        }
    }
}
