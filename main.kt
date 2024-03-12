
import kotlin.io.readLine
import kotlin.text.toInt
import java.io.File
import java.io.FileNotFoundException
import kotlin.concurrent.thread


//Dictionary object
object CryptoData{
    var cryptoData= mutableMapOf<String,Map<String,Any>>()
}

//population object
class Population(popSize:Int){
    val size=popSize
    var list: MutableList<MutableList<Int>> = mutableListOf<MutableList<Int>>()

    fun sort(){

    }
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
        list.add(innerList)
    }
    population.list=list
}

fun convertJsontoMap(data:String): Map<String,Any>{
    val map=mutableMapOf<String,Any>()
    val pairs=data.removeSurrounding("{","}").split(", ")
    for (pair in pairs){
        val (key, value)= pair.split(": ")
        map[key]=value
    }
    return map

}

fun organizeCryptoInfo(content:String): Int{
    val lines= content.lines()
    for(line in lines){
        val (identifier,dataStr)= line.trim().split(", ")
        val dataMap= convertJsontoMap(dataStr)
        CryptoData.cryptoData[identifier]=dataMap
    }
    return lines.size
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

class selectionAlgorithm(cryptoNum:Int,money:Int,analyzeNumber:Int){
    //population size
    val populationSize=10
    //iterationsnum
    val numIterations=1000
    // mutationrate
    val mutationRate:Double=0.1
    //crossoverRate
    val crossoverRate:Double=0.8

    //Declarations
    val money=money
    val cryptoNum=cryptoNum
    val analyzeNumber=analyzeNumber

    //create population object 
    var population= Population(populationSize)


    //selection function
    fun selection(){

    }

    //crossover function
    fun crossover(crossoverRate:Double){

    }

    //mutate function
    fun mutate(mutateRate:Double){

    }
    //fitness function
    fun fitness(){
        
    }

    //program init
    fun init(){
        //Begin the genetic algorithm
        //get initial population
        inicializatePopulation(analyzeNumber,population)
        //begin aagg
        repeat(numIterations) { 
            selection()
            crossover(crossoverRate)
            mutate(mutationRate)
        }
    }
    
}


fun main(){
    //number of crypto to analyze
    val analyzeNumber=10
    //number of crypto after analyze
    var afterAnalyze:Int

    //begin program
    print("Introduce el número de criptomonedas en el que quieres invertir: ")
    val  cryptoNum= readLine()
    print("\nIntroduce la cantidad de dinero que deseq invertir(USD$): ")
    val  money= readLine()
    afterAnalyze=executePythonInfo(analyzeNumber)
    
    //now we can call select crypto genetic algorithm
    val selAlgorithm= selectionAlgorithm(cryptoNum!!.toInt(),money!!.toInt(),afterAnalyze)
    selAlgorithm.init()

}