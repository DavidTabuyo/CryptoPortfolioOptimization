import java.awt.*
import java.awt.event.*
import java.awt.event.ActionListener
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths
import javax.swing.*
import kotlin.collections.MutableList
import kotlin.io.readLine
import kotlin.math.log2
import kotlin.random.Random
import kotlin.text.toInt

object User {
    // user info
    var cryptoNum: Int = 0
    var money: Int = 0
    var inversionType = InversionType.LOW
    var cryptoList: MutableList<Crypto> = mutableListOf()
    enum class InversionType {
        LOW,
        MEDIUM,
        HIGH
    }
    fun setInversionType() {
        inversionType =
                if (money <= 1000) {
                    InversionType.LOW
                } else if (money > 1000 && money <= 10000) {
                    InversionType.MEDIUM
                } else {
                    InversionType.HIGH
                }
    }
    fun setCryptoNum() {
        if (inversionType == InversionType.LOW) {
            cryptoNum = 5
        } else if (inversionType == InversionType.MEDIUM) {
            cryptoNum = 10
        } else {
            cryptoNum = 15
        }
    }
    fun setPercentages(percentages: MutableList<Int>) {
        for (index in 0..cryptoNum - 1) {
            cryptoList[index].percentage = percentages[index]
        }
    }
}

object CryptoData {
    var cryptoData = mutableListOf<Crypto>()
    override fun toString(): String {
        return cryptoData.joinToString(separator = "\n")
    }
}

class Crypto(
        val id: Int,
        val name: String,
        val price: Double,
        val percentageChange24h: Double,
        val marketCap: Double,
        val volume24h: Double,
        val normVolume24h: Double,
        val normPrice: Double,
        var normMarketCap: Double,
        var risk: Double,
        val maxPrice: Double,
        val minPrice: Double,
        val minVolume: Double,
        val maxVolume: Double,
        var stability: Double,
        var rentability: Double,
        var volatility: Double,
        var percentage: Int,
) {
    init {
        // when init, we calculate risk
        var riskMoreIsBad = calculateRiskMarketCap()
        risk = riskMoreIsBad / (1 + riskMoreIsBad)
        volatility = calculateVolatility()
        stability = calculateStability()
        rentability = calculateRentability()
    }
    fun calculateRiskMarketCap(): Double {
        val weights =
                when (User.inversionType) {
                    User.InversionType.LOW -> Triple(0.4, 0.3, 0.2)
                    User.InversionType.MEDIUM -> Triple(0.3, 0.3, 0.2)
                    User.InversionType.HIGH -> Triple(0.2, 0.3, 0.3)
                }
        val weightPrice = weights.first
        val weightPercentageChange = weights.second
        val weightMarketCap = weights.third
        val weightVolume24h = 0.1

        // normalize market cap, we use 0$ as min and BTC+USDT market cap as max: values at
        // 14/03/2024
        val btcMarketCap = 1432757855151
        val usdtMarketCap = 103185970233
        normMarketCap = normalize(marketCap, 0.0, (btcMarketCap + usdtMarketCap).toDouble())
        // normalize percentage change
        val normalizedPercentageChange = normalizePercentageChange(percentageChange24h)

        // Calculation of the weighted risk factor
        val riskFactor =
                (weightPrice * normPrice) +
                        (weightPercentageChange * normalizedPercentageChange) +
                        (weightMarketCap * normMarketCap) +
                        (weightVolume24h * normVolume24h)
        return riskFactor
    }

    fun calculateRentability(): Double {
        val previusPrice = price / (1 + percentageChange24h / 100)
        return (price - previusPrice) / previusPrice * 100
    }
    fun calculateVolatility(): Double {
        val priceVol = maxPrice - minPrice
        val volumeVol = maxVolume - minVolume
        return priceVol / volumeVol
    }

    fun calculateStability(): Double {
        val actualPriceVol = price - minPrice
        val actualRelationVol = actualPriceVol / volatility

        // we assign a weight to the  actual price
        val actualPriceWeight = 0.2

        // return combination of volatility and actual price
        return (volatility * (1 - actualPriceWeight)) + (actualRelationVol * actualPriceWeight)
    }
    override fun toString(): String {
        return "Crypto(id=$id, name='$name', price=$price, " +
                "percentageChange24h=$percentageChange24h, " +
                "marketCap=$marketCap, volume24h=$volume24h, " +
                "normVolume24h=$normVolume24h, normPrice=$normPrice, " +
                "normMarketCap=$normMarketCap, risk=$risk, stability=$stability)"
    }
    fun getOutput(): String {
        // return output Name+Percentage
        return "$name $percentage%"
    }
}
// population class
class Population(popSize: Int) {
    val size = popSize
    var list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()
}

// class to paint first ggaa

class DividedRectangle(population: Population, numToDisplay: Int) : JPanel() {
    private val numSections = User.cryptoNum
    private val random = java.util.Random()

    // Lista para almacenar las etiquetas de cada división
    private val divisionLabels = mutableListOf<JLabel>()

    init {
        layout = GridLayout(1, numSections + 1)
        preferredSize = Dimension(400, 100)

        for (i in 0 until numSections) {
            val colorPanel =
                    JPanel().apply {
                        background = getRandomColor()
                        layout = GridBagLayout()
                        val textLabel =
                                JLabel(getCryptoName(population.list[0][i])).apply {
                                    horizontalAlignment = SwingConstants.CENTER
                                    verticalAlignment = SwingConstants.CENTER
                                    font = Font("Arial", Font.BOLD, 12)
                                    foreground = Color.WHITE
                                }
                        add(textLabel)
                        divisionLabels.add(textLabel)
                    }
            add(colorPanel)
        }

        val redPanel =
                JPanel().apply {
                    background = Color.RED
                    layout = GridBagLayout()
                    val textLabel =
                            JLabel(numToDisplay.toString()).apply {
                                horizontalAlignment = SwingConstants.CENTER
                                verticalAlignment = SwingConstants.CENTER
                                font = Font("Arial", Font.BOLD, 12)
                                foreground = Color.WHITE
                            }
                    add(textLabel)
                }
        add(redPanel)

        val button =
                JButton("Mostrar Porcentajes").apply {
                    addActionListener(
                            ActionListener {
                                redPanel.isVisible = false
                                this.isVisible = false
                                revalidate()
                                repaint()
                                divisionLabels.forEachIndexed { index, label ->
                                    label.text = User.cryptoList[index].getOutput()
                                }
                            }
                    )
                }
        add(button)
    }

    private fun getRandomColor(): Color {
        return Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }
}

// function to get the crypto name with given int:
fun getCryptoName(index: Int): String {
    var name: String = ""
    for (crypto in CryptoData.cryptoData) {
        if (crypto.id == index) {
            name = crypto.name
        }
    }
    return name
}

// function to get crypto with given int
fun getCrypto(index: Int): Crypto {
    var cryptoResult: Crypto =
            Crypto(
                    -1,
                    "",
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0
            )
    for (crypto in CryptoData.cryptoData) {
        if (crypto.id == index) {
            cryptoResult = crypto
            break
        }
    }
    return cryptoResult
}

// Function to normalize a value to the range of 0 to 1
fun normalize(value: Double, min: Double, max: Double): Double {
    return (value - min) / (max - min)
}

// Function to normalize the percentage change to the range of 0 to 1
fun normalizePercentageChange(percentageChange: Double): Double {
    return (percentageChange + 100) / 200 // Assuming the percentage change ranges from -100 to 100
}

fun convertJsontoMap(data: String): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    val pairs = data.removeSurrounding("{", "}").split(", ")
    for (pair in pairs) {
        val (key, value) = pair.split(":")
        map[key] = value
    }
    return map
}

fun organizeCryptoInfo(content: String): Int {
    val lines = content.lines()
    var count = 0
    for (line in lines) {
        val (_, dataStr) = line.trim().split(", ", limit = 2)
        val dataMap = convertJsontoMap(dataStr)
        val crypto =
                Crypto(
                        id = count,
                        name = dataMap["'Nombre'"] as String,
                        price = dataMap["'Precio'"].toString().toDouble(),
                        percentageChange24h =
                                dataMap["'Cambio_porcentual_24h'"].toString().toDouble(),
                        marketCap = dataMap["'Capitalizacion_mercado'"].toString().toDouble(),
                        volume24h = dataMap["'Volumen_operaciones_24h'"].toString().toDouble(),
                        risk = 0.0,
                        normVolume24h =
                                normalize(
                                        dataMap["'Volumen_operaciones_24h'"].toString().toDouble(),
                                        dataMap["'Volumen_24h_min_historico'"]
                                                .toString()
                                                .toDouble(),
                                        dataMap["'Volumen_24h_max_historico'"].toString().toDouble()
                                ),
                        normPrice =
                                normalize(
                                        dataMap["'Precio'"].toString().toDouble(),
                                        dataMap["'Precio_min_historico'"].toString().toDouble(),
                                        dataMap["'Precio_max_historico'"].toString().toDouble()
                                ),
                        normMarketCap = 0.0,
                        minPrice = dataMap["'Precio_min_historico'"].toString().toDouble(),
                        maxPrice = dataMap["'Precio_max_historico'"].toString().toDouble(),
                        minVolume = dataMap["'Volumen_24h_min_historico'"].toString().toDouble(),
                        maxVolume = dataMap["'Volumen_24h_max_historico'"].toString().toDouble(),
                        stability = 0.0,
                        rentability = 0.0,
                        volatility = 0.0,
                        percentage = 0,
                )
        CryptoData.cryptoData.add(crypto)
        count++
    }
    return count
}

fun executePythonInfo(crypto_num: Int): Int {
    // firs, we look if the file exists, if not, we call python program to generate it
    val currentDirectory = Paths.get("").toAbsolutePath().toString()
    val fileName = "$currentDirectory/data/crypto_data.txt"
    var content = ""
    try {
        content = File(fileName).readLines().joinToString("\n")
        // file exists
    } catch (ex: FileNotFoundException) {
        // file dont exist, we create it with  python script
        println("Creating $fileName from python script...")
        try {
            ProcessBuilder("python3", "src/crypto_data.py", crypto_num.toString()).start()
            // sleep while file is not generated yet
            while (!File(fileName).exists()) {
                Thread.sleep(1000)
            }
            println("crypo_data.txt generated... ")
            content = File(fileName).readLines().joinToString("\n")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    } catch (ex: Exception) {
        println("Error while reading file: ${ex.message}")
    }

    // organize data
    return organizeCryptoInfo(content)
}

class selectionAlgorithm(analyzeNumber: Int) {
    // population size
    val populationSize = 200
    // iterationsnum
    val numIterations = if (User.inversionType == User.InversionType.HIGH) 1000 else 500
    // mutationrate
    val mutationRate: Double = 0.8
    // crossoverRate
    val crossoverRate: Double = 0.8
    // elitism 20%
    val elitism = populationSize / 5
    // frame to paint
    val frame = JFrame("Selected Cryptos")

    // Declarations
    val analyzeNumber = analyzeNumber

    // create population object
    var population = Population(populationSize)

    // function to inicializate population
    fun inicializatePopulation() {
        val size = population.size

        val list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()
        for (i in 0 until size) {
            val innerList = mutableListOf<Int>()
            for (j in 0 until analyzeNumber) {
                innerList.add(j)
            }
            innerList.shuffle()

            list.add(innerList.subList(0, User.cryptoNum))
        }
        population.list = list
    }

    // delete duplicates function
    fun deleteDuplicatedChromosome() {
        val uniqueChromosomes = mutableSetOf<Set<Int>>()
        val populationCopy = mutableListOf<MutableList<Int>>()

        for (chromosome in population.list) {
            val chromosomeSet = chromosome.toSet()
            if (uniqueChromosomes.add(chromosomeSet)) {
                populationCopy.add(chromosome)
            }
        }

        population.list = populationCopy
    }
    // function to generate new chromosomes
    fun generateNewChromosome(): MutableList<Int> {
        val chromosome: MutableList<Int> = mutableListOf()
        for (j in 0 until User.cryptoNum) {
            chromosome.add(j)
        }
        chromosome.shuffle()
        return chromosome
    }

    // selection function
    fun selection() {
        // delete duplicates
        deleteDuplicatedChromosome()
        // sort
        population.list.sortByDescending { aptitude(it) }
        // cut by population size
        if (population.list.size > populationSize) {
            population.list =
                    population.list.dropLast(population.list.size - populationSize).toMutableList()
        } else {
            // if after remove duplicates is less we refill
            for (index in 0 until populationSize - population.list.size) {
                population.list.add(generateNewChromosome())
            }
        }
    }

    // crossover function
    fun crossover() {
        for (index in 0..(population.list.size - 1) / 2 step 2) {
            if (Random.nextDouble() < crossoverRate) {

                val firstChromosome = population.list[index]
                val secondChromosome = population.list[index + 1]
                var firstChild = mutableListOf<Int>()
                var secondChild = mutableListOf<Int>()

                // forward
                for (i in 0..User.cryptoNum - 1) {
                    firstChild.add(firstChromosome[i])
                    firstChild.add(secondChromosome[i])
                }
                // backward
                for (i in User.cryptoNum - 1 downTo 0) {
                    secondChild.add(firstChromosome[i])
                    secondChild.add(secondChromosome[i])
                }
                // delete duplicates
                firstChild = firstChild.distinct().toMutableList()
                secondChild = secondChild.distinct().toMutableList()
                // if is less than User.cryptonum add aleatory
                while (firstChild.size < User.cryptoNum) {
                    var randomNumber = (0 until population.size - 1).random()
                    while (randomNumber in firstChild) {
                        randomNumber = (0 until population.size - 1).random()
                    }
                    firstChild.add(randomNumber)
                }

                while (secondChild.size < User.cryptoNum) {
                    var randomNumber = (0 until population.size - 1).random()
                    while (randomNumber in secondChild) {
                        randomNumber = (0 until population.size - 1).random()
                    }
                    secondChild.add(randomNumber)
                }
                // cut and add to population
                population.list.add(firstChild.subList(0, User.cryptoNum))
                population.list.add(secondChild.subList(0, User.cryptoNum))
            }
        }
    }

    // random excluding chromosome
    fun randomExcluding(exclude: MutableList<Int>): Int {
        var randomNum: Int
        do {
            randomNum = Random.nextInt(0, CryptoData.cryptoData.size)
        } while (exclude.contains(randomNum))

        return randomNum
    }

    // mutate function
    fun mutate() {
        var index: Int = 0
        for (chromosome in population.list) {
            index++
            // elitism
            if (index > elitism) {
                // If  mutation rate
                if (Random.nextDouble() < mutationRate) {
                    val firstRandomMutate = Random.nextInt(0, chromosome.size)
                    val secondRandomMutate = Random.nextInt(0, chromosome.size)
                    chromosome[firstRandomMutate] = randomExcluding(chromosome)
                    chromosome[secondRandomMutate] = randomExcluding(chromosome)
                }
            }
        }
    }

    // functions of fitness function
    fun calculatePortfolioVolatility(cryptoList: MutableList<Crypto>): Double {
        return cryptoList.sumOf { it.volatility }
    }

    fun calculatePortfolioRentability(cryptoList: MutableList<Crypto>): Double {
        return cryptoList.sumOf { it.rentability }
    }

    fun calculatePortfolioStability(cryptoList: List<Crypto>): Double {
        return cryptoList.sumOf { it.stability }
    }

    fun calculatePorfolioRisk(cryptoList: MutableList<Crypto>): Double {
        return cryptoList.sumOf { it.risk }
    }
    fun calculateCorrelation(cryptoList: List<Crypto>): Double {
        // Calcular la media de los precios y los cambios porcentuales
        val avgPrice = cryptoList.map { it.price }.average()
        val avgChange = cryptoList.map { it.percentageChange24h }.average()

        // Calcular la suma de los productos de las desviaciones de la media
        var sumXY = 0.0
        var sumX2 = 0.0
        var sumY2 = 0.0
        for (crypto in cryptoList) {
            sumXY += (crypto.price - avgPrice) * (crypto.percentageChange24h - avgChange)
            sumX2 += (crypto.price - avgPrice) * (crypto.price - avgPrice)
            sumY2 +=
                    (crypto.percentageChange24h - avgChange) *
                            (crypto.percentageChange24h - avgChange)
        }

        // Calcular la correlación de Pearson
        val correlation = sumXY / (Math.sqrt(sumX2) * Math.sqrt(sumY2))
        // adjust range to -1,1
        val adjustedCorrelation = (correlation + 1) / 2
        // adjust range to 1,2
        val scaledCorrelation = (adjustedCorrelation * 1 + 1)
        return scaledCorrelation
    }

    // fitness function
    fun aptitude(chromosome: MutableList<Int>): Double {
        // we use diferent aptitude function depending on the inversionType
        var rentabilityWeight: Double
        var stabilityWeight: Double
        var riskWeight: Double
        var diversificationWeight: Double

        // assign weight depending on inversion type
        if (User.inversionType == User.InversionType.LOW) {
            rentabilityWeight = 0.3
            stabilityWeight = 0.2
            riskWeight = 0.4
            diversificationWeight = 0.1
        } else if (User.inversionType == User.InversionType.MEDIUM) {
            rentabilityWeight = 0.4
            stabilityWeight = 0.3
            riskWeight = 0.2
            diversificationWeight = 0.1
        } else {
            rentabilityWeight = 0.5
            stabilityWeight = 0.2
            riskWeight = 0.2
            diversificationWeight = 0.1
        }
        // change from chromosome  to mutable list of crypto
        val cryptoList: MutableList<Crypto> = mutableListOf()
        for (cryptoChromosome in chromosome) {
            for (cryptoInList in CryptoData.cryptoData) {
                if (cryptoChromosome == cryptoInList.id) {
                    cryptoList.add(cryptoInList)
                }
            }
        }

        // calculate portfolio metrics
        var correlationsSum = calculateCorrelation(cryptoList)
        correlationsSum = if (correlationsSum < 0) 0.0 else correlationsSum
        val diversification =
                if (correlationsSum >= 0) calculatePortfolioVolatility(cryptoList) / correlationsSum
                else 0.0

        // calculate and return total aptitude
        val aptitude =
                (rentabilityWeight * calculatePortfolioRentability(cryptoList)) +
                        (stabilityWeight * calculatePortfolioStability(cryptoList)) +
                        (riskWeight * calculatePorfolioRisk(cryptoList)) +
                        (diversificationWeight * diversification)
        return aptitude
    }
    // function to print chromosome
    fun paintChromosome(index: Int) {
        SwingUtilities.invokeLater {
            frame.contentPane.removeAll()
            frame.add(DividedRectangle(population, index), BorderLayout.CENTER)
            frame.setSize(1500, 300)
            frame.revalidate()
            frame.repaint()
            frame.isVisible = true
            frame.addWindowListener(
                    object : WindowAdapter() {
                        override fun windowClosing(e: WindowEvent?) {
                            super.windowClosing(e)
                            System.exit(0)
                        }
                    }
            )
        }
    }

    // program init
    fun init() {
        // get initial population
        inicializatePopulation()
        var prev: MutableList<Int>
        // begin aagg
        for (i in 1..numIterations) {
            prev = population.list[0]
            selection()
            if (aptitude(prev) != aptitude(population.list[0])) {
                // if has changed we paint it
                paintChromosome(i)
            }
            crossover()
            mutate()
        }
        // when algorthm finish, we have cryptos selected
        println("---Algoritmo Finalizado---")
    }

    fun getList(): MutableList<Crypto> {
        val cryptoList: MutableList<Crypto> = mutableListOf<Crypto>()
        for (index in population.list[0]) {
            cryptoList.add(getCrypto(index))
        }

        return cryptoList
    }
}

class percentageAlgorithm(cryptolist: MutableList<Crypto>) {
    // population size
    val populationSize = 100
    // iterationsnum
    val numIterations = if (User.inversionType == User.InversionType.HIGH) 1000 else 500
    // mutationrate
    val mutationRate: Double = 0.1
    // crossoverRate
    val crossoverRate: Double = 0.8
    // elitism 20%
    val elitism = populationSize / 5
    // list of selected cryptos
    val cryptoList: MutableList<Crypto> = cryptolist
    // create population object
    var population = Population(populationSize)
    // minimum percentage
    val min = if (User.inversionType == User.InversionType.HIGH) 2 else 5
    // max percentage when create
    val max =
            if (User.inversionType == User.InversionType.HIGH) 10
            else if (User.inversionType == User.InversionType.MEDIUM) 20 else 30

    // function to inicializate population
    fun inicializatePopulation() {
        val list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()
        repeat(populationSize) {
            val randomVal = MutableList(User.cryptoNum) { Random.nextInt(min, max) }
            // adjust if is higher
            while (randomVal.sum() > 100) {
                val randomIndex = Random.nextInt(0, User.cryptoNum - 1)
                if (randomVal[randomIndex] != min) {
                    randomVal[randomIndex] -= 1
                }
            }
            // adjust if is lower
            while (randomVal.sum() < 100) {
                val randomIndex = Random.nextInt(0, User.cryptoNum - 1)
                randomVal[randomIndex] += 1
            }
            list.add(randomVal)
        }
        population.list = list
    }

    // function to delete duplicated chromosomes
    fun deleteDuplicatedChromosome() {
        val checkDuplicated: MutableSet<MutableList<Int>> = mutableSetOf()
        val populationCopy: MutableList<MutableList<Int>> = mutableListOf()
        for (chromosome in population.list) {
            if (checkDuplicated.add(chromosome)) {
                populationCopy.add(chromosome)
            }
        }
        population.list = populationCopy
    }
    // function to generate new random chromosome in case of refill
    fun generateNewChromosome(): MutableList<Int> {
        val randomVal = MutableList(User.cryptoNum) { Random.nextInt(min, max) }
        // adjust if is higher
        while (randomVal.sum() > 100) {
            val randomIndex = Random.nextInt(0, User.cryptoNum - 1)
            if (randomVal[randomIndex] != min) {
                randomVal[randomIndex] -= 1
            }
        }
        // adjust if is lower
        while (randomVal.sum() < 100) {
            val randomIndex = Random.nextInt(0, User.cryptoNum - 1)
            randomVal[randomIndex] += 1
        }
        return randomVal
    }

    // selection function
    fun selection() {
        // delete duplicates
        deleteDuplicatedChromosome()
        // sort
        population.list.sortByDescending { aptitude(it) }
        // cut by population size
        if (population.list.size > populationSize) {
            population.list =
                    population.list.dropLast(population.list.size - populationSize).toMutableList()
        } else {
            // if after remove duplicates is less we refill
            for (index in 0 until populationSize - population.list.size) {
                population.list.add(generateNewChromosome())
            }
        }
    }

    fun adjustChromosomes(
            chromosome1: MutableList<Int>,
            chromosome2: MutableList<Int>
    ): Pair<MutableList<Int>, MutableList<Int>> {
        // adjust chromosme 1
        while (chromosome1.sum() > 100) {
            val randomIndex = Random.nextInt(0, chromosome1.size)
            if (chromosome1[randomIndex] != min) {
                chromosome1[randomIndex]--
            }
        }
        while (chromosome1.sum() < 100) {
            val randomIndex = Random.nextInt(0, chromosome1.size)
            chromosome1[randomIndex]++
        }
        // adjust chromosme 2
        while (chromosome2.sum() > 100) {
            val randomIndex = Random.nextInt(0, chromosome2.size)
            if (chromosome2[randomIndex] != min) {
                chromosome2[randomIndex]--
            }
        }
        while (chromosome2.sum() < 100) {
            val randomIndex = Random.nextInt(0, chromosome2.size)
            chromosome2[randomIndex]++
        }
        return Pair(chromosome1, chromosome2)
    }

    // different implementations of crossover
    fun crossoverHalf(
            chromosome1: MutableList<Int>,
            chromosome2: MutableList<Int>
    ): Pair<MutableList<Int>, MutableList<Int>> {
        val newChromosome1 =
                (chromosome1.subList(0, chromosome1.size / 2) +
                                chromosome2.subList(chromosome2.size / 2, chromosome2.size))
                        .toMutableList()
        val newChromosome2 =
                (chromosome2.subList(0, chromosome2.size / 2) +
                                chromosome1.subList(chromosome1.size / 2, chromosome1.size))
                        .toMutableList()

        return adjustChromosomes(newChromosome1, newChromosome2)
    }
    fun crossoverYesNo(
            chromosome1: MutableList<Int>,
            chromosome2: MutableList<Int>
    ): Pair<MutableList<Int>, MutableList<Int>> {
        var newChromosome1: MutableList<Int> = mutableListOf()
        var newChromosome2: MutableList<Int> = mutableListOf()
        for (index in 0 until User.cryptoNum) {
            newChromosome1.add(chromosome1[index])
            newChromosome1.add(chromosome2[index])
            newChromosome2.add(chromosome2[index])
            newChromosome2.add(chromosome1[index])
        }

        return adjustChromosomes(
                newChromosome1.subList(0, User.cryptoNum).toMutableList(),
                newChromosome2.subList(0, User.cryptoNum).toMutableList()
        )
    }

    // crossover
    fun crossover() {
        for (index in 0..(population.list.size - 1) / 2 step 2) {
            if (Random.nextDouble() < crossoverRate) {
                val (son1, son2) = crossoverHalf(population.list[index], population.list[index + 1])
                val (son3, son4) =
                        crossoverYesNo(population.list[index], population.list[index + 1])
                population.list.add(son1)
                population.list.add(son2)
                population.list.add(son3)
                population.list.add(son4)
            }
        }
    }

    // aux function for mutation
    fun getRandomMutated(num: Int): Pair<Int, Int> {
        if (num == min * 2) {
            return Pair(min, min)
        } else if (num == min * 2 + 1) {
            return Pair(min, min + 1)
        } else {
            var random1 = Random.nextInt(min, num / 2)
            var random2 = Random.nextInt(min, num / 2)

            while (random1 + random2 != num) {
                // adjust until 100
                if (Random.nextDouble() < 0.5) {
                    random1++
                } else {
                    random2++
                }
            }

            return Pair(random1, random2)
        }
    }

    // mutate
    fun mutate() {
        for ((index, chromosome) in population.list.withIndex()) {
            // elitism
            if (index > elitism) {
                // If  mutation rate
                if (Random.nextDouble() < mutationRate) {
                    var firstRandomMutate: Int
                    var secondRandomMutate: Int
                    do {
                        firstRandomMutate = Random.nextInt(0, chromosome.size)
                        secondRandomMutate = Random.nextInt(0, chromosome.size)
                    } while (firstRandomMutate == secondRandomMutate)

                    val (firstValue, secondValue) =
                            getRandomMutated(
                                    chromosome[firstRandomMutate] + chromosome[secondRandomMutate]
                            )
                    chromosome[firstRandomMutate] = firstValue
                    chromosome[secondRandomMutate] = secondValue
                }
            }
        }
    }

    // function to calculate fault of diversification in portfolio
    fun calculateShannonEntropy(chromosome: List<Int>): Double {
        val sum = chromosome.sum().toDouble()
        val proportions = chromosome.map { it / sum }
        return proportions.map { -it * log2(it) }.sum()
    }

    // aptitude function
    fun aptitude(chromosome: MutableList<Int>): Double {
        // we use diferent aptitude function depending on the inversionType
        var rentabilityWeight: Double
        var riskWeight: Double
        var diversificationWeight: Double
        // assign weight depending on inversion type
        if (User.inversionType == User.InversionType.LOW) {
            rentabilityWeight = 0.6
            riskWeight = 0.3
            diversificationWeight = 0.5
        } else if (User.inversionType == User.InversionType.MEDIUM) {
            rentabilityWeight = 0.5
            riskWeight = 0.4
            diversificationWeight = 0.5
        } else {
            rentabilityWeight = 0.4
            riskWeight = 0.5
            diversificationWeight = 0.5
        }
        // max rentability
        var totalRent = 0.0
        for (i in 0 until chromosome.size) {
            totalRent += cryptoList[i].rentability * chromosome[i] / 100
        }
        // minimize risk
        var totalRisk = 0.0
        for (i in 0 until chromosome.size) {
            totalRisk += cryptoList[i].risk * chromosome[i] / 100
        }
        // penalize diversification fault
        val diversificationPenalty = calculateShannonEntropy(chromosome)
        // calculate aptitude function
        val fitness =
                totalRent * rentabilityWeight - totalRisk * riskWeight +
                        diversificationPenalty * diversificationWeight
        // return value
        return fitness
    }

    fun init() {
        // inicializate population
        inicializatePopulation()
        // begin aagg
        println("---Empieza el algoritmo de porcentajes---")
        repeat(numIterations) {
            selection()
            crossover()
            mutate()
        }
        User.setPercentages(population.list[0])
        println("---Algoritmo Finalizado---")
    }
}

fun main() {
    // number of crypto to analyze
    val analyzeNumber = 100
    // number of crypto after analyze
    var afterAnalyze: Int

    // begin program
    var money: String?
    do {
        print("\nIntroduce la cantidad de dinero que desea invertir(USD$): ")
        money = readLine()
    } while (money!!.toInt() <= 0)

    // Asign to object User
    User.money = money.toInt()
    User.setInversionType()
    User.setCryptoNum()
    afterAnalyze = executePythonInfo(analyzeNumber)
    // now we can call select crypto genetic algorithm
    val selAlgorithm = selectionAlgorithm(afterAnalyze)
    selAlgorithm.init()
    User.cryptoList = selAlgorithm.getList()
    val percentageAlgorithm = percentageAlgorithm(User.cryptoList)
    percentageAlgorithm.init()
}
