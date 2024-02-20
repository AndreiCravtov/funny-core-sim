package engineering.fs

class CPU(private val program: List<Instruction>) {
    private val registers = IntArray(16)
    private var pc = 0
    private var cycleCount = 0

    fun run(initialRegisters: List<Int> = (1..16).map { 0 }): RunResult {
        if (initialRegisters.size != registers.size)
            throw IllegalArgumentException("Supplied initial register is of wrong size. Should be ${registers.size}, found ${initialRegisters.size}")

        // set initial values
        initialRegisters.forEachIndexed { i, v -> registers[i] = v }
        pc = 0
        cycleCount = 0

        // run program
        while (pc < program.size) {
            val oldPc = pc

            val instruction = program[pc]
            val error = processInstruction(instruction)
            if (error != null)
                return RunResult.Error(error)

            if (oldPc == pc)
                pc++
        }

        // return results
        return RunResult.Ok(registers.toList(), cycleCount)
    }

    private fun processInstruction(instruction: Instruction): ProcessError? {
        // count cycles
        cycleCount += instruction.cycles

        // interpret instruction
        return when (instruction) {
            is Instruction.Add -> {
                try {
                    registers[instruction.target.index] = Math.addExact(
                        registers[instruction.source1.index],
                        registers[instruction.source2.index])
                    null
                } catch (_: ArithmeticException) {
                    ProcessError.NUMERIC_OVERFLOW
                }
            }
            is Instruction.Sub -> {
                try {
                    registers[instruction.target.index] = Math.subtractExact(
                        registers[instruction.source1.index],
                        registers[instruction.source2.index])
                    null
                } catch (_: ArithmeticException) {
                    ProcessError.NUMERIC_OVERFLOW
                }
            }
            is Instruction.Mul -> {
                try {
                    registers[instruction.target.index] = Math.multiplyExact(
                        registers[instruction.source1.index],
                        registers[instruction.source2.index])
                    null
                } catch (_: ArithmeticException) {
                    ProcessError.NUMERIC_OVERFLOW
                }
            }
            is Instruction.Shl -> {
                registers[instruction.target.index] =
                    registers[instruction.target.index] shl
                            registers[instruction.source.index]
                null
            }
            is Instruction.Shr -> {
                registers[instruction.target.index] =
                    registers[instruction.target.index] shr
                            registers[instruction.source.index]
                null
            }
            is Instruction.Bor -> {
                registers[instruction.target.index] =
                    registers[instruction.source1.index] or
                            registers[instruction.source1.index]
                null
            }
            is Instruction.Band -> {
                registers[instruction.target.index] =
                    registers[instruction.source1.index] and
                            registers[instruction.source1.index]
                null
            }
            is Instruction.Li -> {
                registers[instruction.target.index] =
                    instruction.value.toInt()
                null
            }
            is Instruction.Jz -> {
                if (registers[instruction.condition.index] == 0) {
                    try {
                        program[pc + instruction.lines]
                        pc += instruction.lines
                        null
                    } catch (_: IndexOutOfBoundsException) {
                        ProcessError.UNDEFINED_JUMP
                    }
                } else null
            }
            is Instruction.Jp -> {
                if (registers[instruction.condition.index] > 0) {
                    try {
                        program[pc + instruction.lines]
                        pc += instruction.lines
                        null
                    } catch (_: IndexOutOfBoundsException) {
                        ProcessError.UNDEFINED_JUMP
                    }
                } else null
            }
        }
    }
}

enum class ProcessError {
    NUMERIC_OVERFLOW, UNDEFINED_JUMP
}

sealed interface RunResult {
    data class Ok(val registers: List<Int>, val cycleCount: Int): RunResult
    data class Error(val reason: ProcessError): RunResult
}