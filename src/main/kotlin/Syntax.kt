package engineering.fs

sealed class Token() {
    abstract val lexeme: String
    data object Space: Token() { override val lexeme: String = " " }
    data object NewLine: Token() { override val lexeme: String = "\\n" }
    data object Add: Token() { override val lexeme: String = "add" }
    data object Mul: Token() { override val lexeme: String = "mul" }
    data object Sub: Token() { override val lexeme: String = "sub" }
    data object Shl: Token() { override val lexeme: String = "shl" }
    data object Shr: Token() { override val lexeme: String = "shr" }
    data object Bor: Token() { override val lexeme: String = "bor" }
    data object Band: Token() { override val lexeme: String = "band" }
    data object Li: Token() { override val lexeme: String = "li" }
    data object Jz: Token() { override val lexeme: String = "jz" }
    data object Jp: Token() { override val lexeme: String = "jp" }
    data class Num(override val lexeme: String): Token()
}

sealed class Instruction(val cycles: Int) {
    data class Add(val target: Register, val source1: Register, val source2: Register): Instruction(1)
    data class Sub(val target: Register, val source1: Register, val source2: Register): Instruction(1)
    data class Mul(val target: Register, val source1: Register, val source2: Register): Instruction(3)
    data class Shl(val target: Register, val source: Register): Instruction(1)
    data class Shr(val target: Register, val source: Register): Instruction(1)
    data class Bor(val target: Register, val source1: Register, val source2: Register): Instruction(1)
    data class Band(val target: Register, val source1: Register, val source2: Register): Instruction(1)
    data class Li(val target: Register, val value: Short): Instruction(1)
    data class Jz(val condition: Register, val lines: Short): Instruction(3)
    data class Jp(val condition: Register, val lines: Short): Instruction(3)
}

data class Register(val index: Int) {
    init {
        if (index !in 0..15)
            throw IllegalArgumentException("Register index out of range")
    }

    override fun toString() = index.toString()
}