package engineering.fs

import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

infix fun DecimalFormat.f(d: Double) = format(d)
infix fun Int.closest(n: Int) = this / n * n

fun testDiv(source: String,
            stressIterations: Int = 1_000_000,
            dataIterations: Int = 100_000,
            minQuotient: Int = 20_000,
            groupSize: Int = 20,
            barSize: Int = 150) {
    // Load program
    val program = parseTokens(parseString(source))
    val cpu = CPU(program)

    // Stress testing and diagnostics
    repeat(stressIterations) {
        val dividend = Random.nextInt(0, 0xFFFF + 1)
        val divisor = Random.nextInt(1, 0xFFFF + 1)
        val quotient = ((dividend.toLong() and 0xFFFF) / (divisor.toLong() and 0xFFFF)).toInt()
        val remainder = if (dividend == 0) 0 else ((dividend.toLong() and 0xFFFF) % (divisor.toLong() and 0xFFFF)).toInt()

        when (val result = cpu.run(listOf(dividend,divisor,0,0,0,0,0,0,0,0,0,0,0,0,0,0))) {
            is RunResult.Error -> throw RuntimeException("Failed to compute correct result. Expected $dividend÷$divisor=($quotient rem $remainder); got runtime error ${result.reason}")
            is RunResult.Ok -> {
                val cQuot = result.registers[2]
                val cRem = result.registers[3]
                if (quotient != cQuot || remainder != cRem)
                    throw RuntimeException("Failed to compute correct result. Expected $dividend÷$divisor=($quotient rem $remainder); got ($cQuot rem $cRem)")
            }
        }
    }

    // Data collection
    val rawData = mutableListOf<Int>()
    val minDividend = max(1, minQuotient)
    while (rawData.size < dataIterations) { // number of iterations
        // generate model solution
        val dividend = Random.nextInt(minDividend, 0xFFFF + 1)
        val divisor = Random.nextInt(1, dividend / minDividend + 1)
        val quotient = ((dividend.toLong() and 0xFFFF) / (divisor.toLong() and 0xFFFF)).toInt()
        val remainder = ((dividend.toLong() and 0xFFFF) % (divisor.toLong() and 0xFFFF)).toInt()

        when (val result = cpu.run(listOf(dividend,divisor,0,0,0,0,0,0,0,0,0,0,0,0,0,0))) {
            is RunResult.Error -> throw RuntimeException("Failed to compute correct result. Expected $dividend÷$divisor=($quotient rem $remainder); got runtime error ${result.reason}")
            is RunResult.Ok -> {
                val cQuot = result.registers[2]
                val cRem = result.registers[3]
                if (quotient != cQuot || remainder != cRem)
                    throw RuntimeException("Failed to compute correct result. Expected $dividend÷$divisor=($quotient rem $remainder); got ($cQuot rem $cRem)")

                // add to cycle count
                rawData.add(result.cycleCount)
            }
        }
    }

    // Data analysis and reporting
    rawData.sort()
    val min = rawData.first()
    val median = rawData[rawData.size/2]
    val max = rawData.last()
    val mean = rawData.average()
    val stdDev =
        if (rawData.isEmpty()) 0.0
        else sqrt(rawData.sumOf { (it - mean) * (it - mean) } / rawData.size)
    val df = DecimalFormat("#.##")
    println("After running ${"%,d".format(rawData.size)} test iterations:")
    println("Summary: minimum=$min, median=$median, maximum=$max, mean=${df f mean}, standard deviation=${df f stdDev}")

    val groupedData = rawData.groupBy { it closest groupSize }.mapValues { (_, l) -> l.size }
    val largestFreq = groupedData.values.max()
    println("Cycle count distribution visualisation:")
    for ((count, freq) in groupedData)
        println("$count-${count+groupSize-1} : ${"❘".repeat((freq.toDouble() / largestFreq * barSize).toInt())}")
}

fun main() {
    val source = """
        your code goes here :)
    """.trimIndent()

    testDiv(source)
}