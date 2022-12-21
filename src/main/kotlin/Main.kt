import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

val separator = File.separator
lateinit var vcs: File
lateinit var commits: File
lateinit var config: File
lateinit var index: File
lateinit var log: File

fun main(args: Array<String>) {
    initFiles()

    when (val commend = if (args.isEmpty()) "--help" else args[0]) {
        "--help" -> commendHelp()
        "config" -> config(config, inputOption(args))
        "add" -> add(index, inputOption(args))
        "log" -> log(log)
        "commit" -> commit(log, config, index, inputOption(args))
        "checkout" -> checkout(inputOption(args))
        else -> println("'$commend' is not a SVCS command.")
    }
}

fun initFiles() {
    vcs = File("vcs")
    if (!vcs.exists()) vcs.mkdir()

    commits = File("vcs${separator}commits")
    if (!commits.exists()) commits.mkdir()

    val configPath = "vcs${separator}config.txt"
    val indexPath = "vcs${separator}index.txt"
    val logPath = "vcs${separator}log.txt"

    config = File(configPath)
    if (!config.exists()) config.createNewFile()

    index = File(indexPath)
    if (!index.exists()) index.createNewFile()

    log = File(logPath)
    if (!log.exists()) log.createNewFile()
}

fun checkout(option: String?) {
    if (option == null) {
        println("Commit id was not passed.")
    } else {
        val commitId = File("$commits/$option")
        if (commitId.exists()) {
            println("Switched to commit $option.")
            val targetDirectory = vcs
            commitId.copyRecursively(targetDirectory, overwrite = true)
        } else {
            println("Commit does not exist.")
        }
    }

}

fun log(logFile: File) {
    if (logFile.readText().isEmpty()) {
        println("No commits yet.")
    } else {
        println(logFile.readText())
        println()
    }
}

fun config(fileConfig: File, option: String?) {
    fileConfig.run {
        if (option == null) {
            if (readText().isEmpty()) println("Please, tell me who you are.")
            else println("The username is ${readText()}.")
        } else {
            println("The username is $option.")
            writeText(option)
        }
    }
}

fun add(fileAdd: File, option: String?) {
    fileAdd.run {
        if (option == null) {
            if (readText().isEmpty()) println("Add a file to the index.")
            else println("Tracked files:\n${readText()}")
        } else {
            if (!File(option).exists()) println("Can't find '$option'.")
            else {
                println("The file '$option' is tracked.")
                readText().let { writeText(if (it.isEmpty()) option else it + "\n$option") }
            }
        }
    }
}

fun commit(fileLog: File, fileConfig: File, indexFile: File, option: String?) {
    if (option == null) {
        println("Message was not passed.")
    } else if (buildID(indexFile) == lastCommitID(fileLog)) {
        println("Nothing to commit.")
    } else {
        val commitID = buildID(indexFile)
        fileLog.readText().run {
            fileLog.writeText("${commitInfo(fileConfig, commitID, option)}\n$this")
        }
        println("Changes are committed.")
        val commitDir = File("vcs/commits/$commitID")
        commitDir.mkdir()
        indexFile.readLines().forEach {
            File(it).copyTo(File("vcs/commits/$commitID/$it"))
        }
    }
}

fun commendHelp() = println(
    """
        These are SVCS commands:
        config     Get and set a username.
        add        Add a file to the index.
        log        Show commit logs.
        commit     Save changes.
        checkout   Restore a file.
    """.trimIndent()
)

fun lastCommitID(logFile: File): String {
    logFile.readLines().run {
        return if (this.isEmpty()) " " else this.first().substringAfter(' ')
    }
}

fun inputOption(args: Array<String>) = if (args.size == 2) args[1] else null

fun commitInfo(fileConfig: File, hashID: String, option: String) = """
            commit $hashID
            Author: ${fileConfig.readText()}
            $option
            
        """.trimIndent()

fun buildID(indexFile: File) = md5(indexFile.readLines().joinToString { File(it).readText() })


fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
}