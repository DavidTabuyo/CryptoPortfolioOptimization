import java.io.File
import java.io.FileNotFoundException
import kotlin.io.readLine
import kotlin.random.Random
import kotlin.text.toInt

object User {
    // user info
    var cryptoNum: Int = 0
    var money: Int = 0
    var inversionType = InversionType.LOW
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
}

object CryptoData {
    var cryptoData = mutableListOf<Crypto>()
    override fun toString(): String {
        return cryptoData.joinToString(separator = "\n")
    }
}
// population object
class Population(popSize: Int) {
    val size = popSize
    var list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()
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
                )
        CryptoData.cryptoData.add(crypto)
        count++
    }
    return count
}

fun executePythonInfo(crypto_num: Int): Int {
    // firs, we look if the file exists, if not, we call python program to generate it
    val fileName = "crypto_data.txt"
    var content = ""
    try {
        content = File(fileName).readLines().joinToString("\n")
        // file exists
    } catch (ex: FileNotFoundException) {
        // file dont exist, we create it with  python script
        println("Creating $fileName from python script...")
        try {
            ProcessBuilder("python3", "crypto_data.py", crypto_num.toString()).start()
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
    val populationSize = 10
    // iterationsnum
    val numIterations = 10
    // mutationrate
    val mutationRate: Double = 0.5
    // crossoverRate
    val crossoverRate: Double = 0.8
    // elitism 20%
    val elitism = populationSize / 5

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
        val uniqueChromosomes = mutableSetOf<String>()
        val population_copy = mutableListOf<MutableList<Int>>()
        for (chromosome in population.list) {

            val sortedChromosome = chromosome.sorted().joinToString(",")
            // add if not duplicated
            if (uniqueChromosomes.add(sortedChromosome)) {
                population_copy.add(chromosome)
            }
            population.list = population_copy
        }
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
        population.list.sortByDescending { aptitude(it) }
        // delete duplicates
        deleteDuplicatedChromosome()
        // cut by population size
        if (population.list.size > populationSize) {
            population.list.dropLast(population.list.size - populationSize)
        } else {
            // if after remove duplicates is less we refill
            for (index in 0 until populationSize - population.list.size) {
                population.list.add(generateNewChromosome())
            }
        }
    }

    // crossover function
    fun crossover() {}

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

    // program init
    fun init() {
        // get initial population
        inicializatePopulation()
        // begin aagg
        println(population.list)
        repeat(numIterations) {
            selection()
            crossover()
            mutate()
        }
        println(population.list)
    }
}

fun main() {
    // number of crypto to analyze
    val analyzeNumber = 20
    // number of crypto after analyze
    var afterAnalyze: Int

    // begin program
    print("\nIntroduce la cantidad de dinero que desea invertir(USD$): ")
    val money = readLine()
    // Asign to object User
    User.money = money!!.toInt()
    User.setInversionType()
    User.setCryptoNum()
    afterAnalyze = executePythonInfo(analyzeNumber)
    // now we can call select crypto genetic algorithm
    val selAlgorithm = selectionAlgorithm(afterAnalyze)
    selAlgorithm.init()
}
