package engineering.fs

fun parseString(source: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var cursor = 0

    fun withinSource(index: Int = cursor) = index in source.indices
    fun peek(i: Int = cursor) = if (withinSource(i)) source[i] else null
    fun debugPeek(ahead: Int = 5) = (cursor range (cursor + ahead)).map {
        when (val c = peek(it)) {
            null -> ""
            '\n' -> "\\n"
            else -> c } }
        .joinToString("")
    fun step() = if (withinSource()) source[cursor++] else null
    fun step(n: Int) = (1..n).mapNotNull { step() }.joinToString("")

    while (withinSource()) {
        when (val c = step()) {
            '#' -> { // ignore comments
                while (withinSource() && peek() != '\n')
                    step()
            }
            ' ' -> tokens.add(Token.Space)
            '\n' -> tokens.add(Token.NewLine)
            'a' -> { // expect `add`
                val next = step(2)
                if (next != "dd")
                    throw RuntimeException("Expected: add; found: a$next${debugPeek()}...")
                tokens.add(Token.Add)
            }
            'm' -> { // expect `mul`
                val next = step(2)
                if (next != "ul")
                    throw RuntimeException("Expected: mul; found: m$next${debugPeek()}...")
                tokens.add(Token.Mul)
            }
            'l' -> { // expect `li`
                val next = step(1)
                if (next != "i")
                    throw RuntimeException("Expected: li; found: l$next${debugPeek()}...")
                tokens.add(Token.Li)
            }
            'j' -> { // expect `jz` or `jp`
                val next = step(1)
                if (next != "z" && next != "p")
                    throw throw RuntimeException("Expected: jz or jp; found: j$next${debugPeek()}...")

                if (next == "z")
                    tokens.add(Token.Jz)
                else tokens.add(Token.Jp)
            }
            'b' -> {
                when (val c2 = step()) {
                    'o' -> { // expect `bor`
                        val next = step(1)
                        if (next != "r")
                            throw RuntimeException("Expected: bor; found: bo$next${debugPeek()}...")
                        tokens.add(Token.Bor)
                    }
                    'a' -> { // expect `band`
                        val next = step(2)
                        if (next != "nd")
                            throw RuntimeException("Expected: band; found: ba$next${debugPeek()}...")
                        tokens.add(Token.Band)
                    }
                    else -> throw throw RuntimeException("Expected: bor or band; found: b$c2${debugPeek()}...")
                }
            }
            's' -> {
                when (val c2 = step()) {
                    'u' -> { // expect `sub`
                        val next = step(1)
                        if (next != "b")
                            throw RuntimeException("Expected: sub; found: su$next${debugPeek()}...")
                        tokens.add(Token.Sub)
                    }
                    'h' -> {
                        val next = step(1)
                        if (next != "l" && next != "r")
                            throw throw RuntimeException("Expected: shl or shr; found: sh$next${debugPeek()}...")

                        if (next == "l")
                            tokens.add(Token.Shl)
                        else tokens.add(Token.Shr)
                    }
                    else -> throw throw RuntimeException("Expected: sub or shl or shr; found: s$c2${debugPeek()}...")
                }
            }
            '0' -> tokens.add(Token.Num("0"))
            '-', in '1'..'9' -> { // expect signed integer
                if (c !in '1'..'9' && peek() !in '1'..'9')
                    throw RuntimeException("Expected: signed integer; found: $c${debugPeek()}...")

                var lexeme = c.toString() + if (c == '-') step(1) else ""
                while (peek() in '0'..'9')
                    lexeme += step()
                tokens.add(Token.Num(lexeme))
            }
            else -> throw RuntimeException("Found unexpected: $c${debugPeek()}...")
        }
    }

    return tokens
}

fun parseTokens(source: List<Token>): List<Instruction> {
    val instructions = mutableListOf<Instruction>()
    var cursor = 0

    fun withinSource(index: Int = cursor) = index in source.indices
    fun peek(i: Int = cursor) = if (withinSource(i)) source[i] else null
    fun debugPeek(ahead: Int = 5) = (cursor range (cursor + ahead))
        .mapNotNull { peek(it) }
        .joinToString("") { it.lexeme }
    fun step() = if (withinSource()) source[cursor++] else null
    fun consumeSpace(): Boolean {
        if (peek() != Token.Space) return false
        while (withinSource() && peek() == Token.Space)
            step()
        return true
    }
    fun num() =
        if (peek() is Token.Num) step() as Token.Num
        else throw RuntimeException("Expected: signed integer; found ${debugPeek()}")
    fun i16(): Short {
        val t = num()
        try {
            return t.lexeme.toShort()
        } catch (_: Exception) {
            throw RuntimeException("Expected: 16-bit signed integer; found ${t.lexeme}${debugPeek()}")
        }
    }
    fun reg(): Register {
        val t = num()
        try {
            val n = t.lexeme.toInt()
            if (n !in 0..15)
                throw RuntimeException()
            return Register(n)
        } catch (_: NumberFormatException) {
            throw Exception("Expected: register index from 0 to 15; found ${t.lexeme}${debugPeek()}")
        }
    }
    while (withinSource()) {
        when (val t = step()) {
            Token.Space, Token.NewLine -> {} // ignore whitespace

            // parse instruction
            Token.Li, Token.Jz, Token.Jp,
            Token.Shl, Token.Shr,
            Token.Add, Token.Sub, Token.Mul, Token.Bor, Token.Band -> {
                if (!consumeSpace())
                    throw RuntimeException("Expected: space; found: ${debugPeek()}...")
                val r1 = reg()
                if (!consumeSpace())
                    throw RuntimeException("Expected: space; found: ${debugPeek()}...")
                when (t) {
                    Token.Li, Token.Jz, Token.Jp -> {
                        val num = i16()
                        when (t) {
                            Token.Li -> instructions.add(Instruction.Li(r1, num))
                            Token.Jz -> instructions.add(Instruction.Jz(r1, num))
                            else -> instructions.add(Instruction.Jp(r1, num))
                        }
                    }
                    else -> {
                        val r2 = reg()
                        when (t) {
                            Token.Shl -> instructions.add(Instruction.Shl(r1, r2))
                            Token.Shr -> instructions.add(Instruction.Shr(r1, r2))
                            else -> {
                                if (!consumeSpace())
                                    throw RuntimeException("Expected: space; found: ${debugPeek()}...")
                                val r3 = reg()
                                when (t) {
                                    Token.Add -> instructions.add(Instruction.Add(r1, r2, r3))
                                    Token.Sub -> instructions.add(Instruction.Sub(r1, r2, r3))
                                    Token.Mul -> instructions.add(Instruction.Mul(r1, r2, r3))
                                    Token.Bor -> instructions.add(Instruction.Bor(r1, r2, r3))
                                    else -> instructions.add(Instruction.Band(r1, r2, r3))
                                }
                            }
                        }
                    }
                }
                consumeSpace()
                if (withinSource() && peek() != Token.NewLine)
                    throw RuntimeException("Expected: new line; found: ${debugPeek()}...")
            }
            else -> throw RuntimeException("Found unexpected: ${t!!.lexeme}${debugPeek()}...")
        }
    }

    return instructions
}

infix fun Int.range(other: Int) =
    if (this < other) this..other
    else other..this