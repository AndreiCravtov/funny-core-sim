package engineering.fs

import kotlin.math.sqrt
import kotlin.random.Random

fun testDiv(source: String) {
    // Load program
    val program = parseTokens(parseString(source))
    val cpu = CPU(program)

    // Testing and diagnostics
    val data = mutableListOf<Int>()
    repeat(100_000) { // number of iterations
        // generate model solution
        val dividend = Random.nextInt() and 0xFFFF
        var divisor = Random.nextInt() and 0xFFFF
        while (divisor == 0)
            divisor = Random.nextInt() and 0xFFFF
        val quotient = ((dividend.toLong() and 0xFFFF) / (divisor.toLong() and 0xFFFF)).toInt()
        val remainder = ((dividend.toLong() and 0xFFFF) % (divisor.toLong() and 0xFFFF)).toInt()

        when (val result = cpu.run(listOf(dividend,divisor,0,0,0,0,0,0,0,0,0,0,0,0,0,0))) {
            is RunResult.Error -> throw RuntimeException("Runtime error occurred ${result.reason}")
            is RunResult.Ok -> {
                val cQuot = result.registers[2]
                val cRem = result.registers[3]
                if (quotient != cQuot || remainder != cRem)
                    throw RuntimeException("Failed to compute correct result. Expected $dividend÷$divisor=($quotient rem $remainder); got ($cQuot rem $cRem)")

                // add to cycle count
                data.add(result.cycleCount)
            }
        }
    }

    // Data analysis and reporting
    val avg = data.average()
    val stdDev =
        if (data.isEmpty()) 0
        else sqrt(data.sumOf { (it - avg) * (it - avg) } / data.size)
    println("After running ${data.size} test iterations, your average cycles per division is $avg, with a standard deviation of $stdDev")
}

fun main() {
    val source = """
        your code here :)
    """.trimIndent()

    testDiv(source)
}
