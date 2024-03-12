
import kotlin.io.readLine
import kotlin.text.toInt
import java.io.File
import java.io.FileNotFoundException
import kotlin.concurrent.thread

/*
Añadir los datos normalizados leidos del txt -> añadirlos al objeto + para market cap usar(0,valor del dolar). Así estaría la population acabada

*/



object User{
    //user info
    var cryptoNum:Int=0
    var money:Int=0
    var inversionType = InversionType.LOW
    enum class InversionType {
        LOW,
        MEDIUM,
        HIGH
    }
    fun setInversionType(){
        inversionType=if(money <= 1000){
            InversionType.LOW
        }else if (money >1000 && money<=10000){
            InversionType.MEDIUM
        }else{
            InversionType.HIGH
        } 
    }
    fun setCryptoNum(){
        if (inversionType == InversionType.LOW) {
            cryptoNum=5
        } else if (inversionType == InversionType.MEDIUM) {
            cryptoNum=10
        } else {
            cryptoNum=15
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
    var risk: Double 
    
){
    init{
        //when init, we calculate risk
        risk=calculateRisk()
    }
    fun calculateRisk():Double{
        val weights = when (User.inversionType) {
            User.InversionType.LOW -> Triple(0.4, 0.3, 0.2)
            User.InversionType.MEDIUM -> Triple(0.3, 0.3, 0.2)
            User.InversionType.HIGH -> Triple(0.2, 0.3, 0.3)
        }
        val weightPrice = weights.first
        val weightPercentageChange = weights.second
        val weightMarketCap = weights.third
        val weightVolume24h = 0.1 


        // Data normalization (assuming each metric is normalized to a range of 0 to 1)
        val normalizedPrice = normalize(price,0.0,1.0) // Normalize price
        val normalizedPercentageChange = normalizePercentageChange(percentageChange24h) // Normalize percentage change
        val normalizedMarketCap = normalize(marketCap,0.0,1.0) // Normalize market cap
        val normalizedVolume24h = normalize(volume24h,0.0,1.0) // Normalize volume 24h
        
        // Calculation of the weighted risk factor
        val riskFactor = (weightPrice * normalizedPrice) + (weightPercentageChange * normalizedPercentageChange) +
                (weightMarketCap * normalizedMarketCap) + (weightVolume24h * normalizedVolume24h)
        return riskFactor
    }

    override fun toString(): String {
        return "Crypto(id=$id, name='$name', price=$price, " +
               "percentageChange24h=$percentageChange24h, " +
               "marketCap=$marketCap, volume24h=$volume24h, risk=$risk)"
    }
}

object CryptoData {
    var cryptoData = mutableListOf<Crypto>()
    override fun toString(): String {
        return cryptoData.joinToString(separator = "\n")
    }
}
//population object
class Population(popSize:Int){
    val size=popSize
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

fun inicializatePopulation(chromosomeLength:Int, population:Population){
    val size= population.size
    val list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()
    for (i in 0 until size){
        val innerList = mutableListOf<Int>()
        for (j in 0 until chromosomeLength){
            innerList.add(j)
        }
        innerList.shuffle()

        list.add(innerList.subList(0,User.cryptoNum))
    }
    population.list=list
}

fun convertJsontoMap(data:String): Map<String,Any>{
    val map=mutableMapOf<String,Any>()
    val pairs=data.removeSurrounding("{","}").split(", ")
    for (pair in pairs){
        val (key, value)= pair.split(":")
        map[key]=value
    }
    return map

}

fun organizeCryptoInfo(content: String): Int {
    val lines = content.lines()
    var count = 0
    for (line in lines) {
        val (_, dataStr) = line.trim().split(", ", limit=2)
        val dataMap = convertJsontoMap(dataStr)
        val crypto = Crypto(
            id = count, 
            name = dataMap["'Nombre'"] as String,
            price = dataMap["'Precio'"].toString().toDouble(),
            percentageChange24h = dataMap["'Cambio_porcentual_24h'"].toString().toDouble(),
            marketCap = dataMap["'Capitalizacion_mercado'"].toString().toDouble(),
            volume24h = dataMap["'Volumen_operaciones_24h'"].toString().toDouble(),
            risk = 0.0 
        )
        CryptoData.cryptoData.add(crypto)
        count++
    }
    return count
}


fun executePythonInfo(crypto_num:Int):Int{
    //firs, we look if the file exists, if not, we call python program to generate it
    val fileName = "crypto_data.txt"
    var content= ""
    try {
        content = File(fileName).readLines().joinToString("\n")
        //file exists
    } catch (ex: FileNotFoundException) {
        //file dont exist, we create it with  python script
        println("Creating $fileName from python script...")
        try{
            ProcessBuilder("python3","crypto_data.py",crypto_num.toString()).start()
            //sleep 5 seconds to generate file
            Thread.sleep(5000)
            println("crypo_data.txt generated... ")
            content = File(fileName).readLines().joinToString("\n")
        }catch(ex:Exception){
            ex.printStackTrace()
        }
    

    } catch (ex: Exception) {
        println("Error while reading file: ${ex.message}")
    }

    // organize data
    return organizeCryptoInfo(content)
}

class selectionAlgorithm(analyzeNumber:Int){
    //population size
    val populationSize=10
    //iterationsnum
    val numIterations=1000
    // mutationrate
    val mutationRate:Double=0.1
    //crossoverRate
    val crossoverRate:Double=0.8

    //Declarations
    val analyzeNumber=analyzeNumber

    //create population object 
    var population= Population(populationSize)


    //selection function
    fun selection(){
        population.list.sortByDescending { aptitude(it) }
    }

    //crossover function
    fun crossover(crossoverRate:Double){
        
    }

    //mutate function
    fun mutate(mutateRate:Double){

    }
    //fitness function
    fun aptitude(chromosome:MutableList<Int>):Int{
        //we use diferent aptitude function depending on the inversionType
        var rentabilityWeight:Double
        var stabilityWeight:Double
        var riskWeight:Double
        var diversificationWeight:Double

        //assign weight depending on inversion type
        if(User.inversionType==User.InversionType.LOW){
            rentabilityWeight=0.3
            stabilityWeight=0.2
            riskWeight=0.4
            diversificationWeight=0.1

        }else if(User.inversionType==User.InversionType.MEDIUM){
            rentabilityWeight=0.4
            stabilityWeight=0.3
            riskWeight=0.2
            diversificationWeight=0.1

        }else{
            rentabilityWeight=0.5
            stabilityWeight=0.2
            riskWeight=0.2
            diversificationWeight=0.1
        }

        return 0
    }

    //program init
    fun init(){
        //Begin the genetic algorithm
        //get initial population
        inicializatePopulation(analyzeNumber,population)
        //begin aagg
        println(population.list)
        repeat(numIterations) { 
            selection()
            crossover(crossoverRate)
            mutate(mutationRate)
        }
    }
    
}


fun main(){
    //number of crypto to analyze
    val analyzeNumber=20
    //number of crypto after analyze
    var afterAnalyze:Int

    //begin program
    print("\nIntroduce la cantidad de dinero que desea invertir(USD$): ")
    val  money= readLine()
    //Asign to object User
    User.money=money!!.toInt()
    User.setInversionType()
    User.setCryptoNum()
    afterAnalyze=executePythonInfo(analyzeNumber)
    //now we can call select crypto genetic algorithm
    print(CryptoData.toString())
    val selAlgorithm= selectionAlgorithm(afterAnalyze)
    selAlgorithm.init()

}