#!/bin/bash

readonly PKG="at.caspase.rxdroid"
readonly DTEMP="/mnt/sdcard/"
readonly MISC="scripts/misc/"

DEBUG=0

die() {
	if [[ $# -ne 0 ]]; then
		echo $* >&2
	else
		echo "Error" >&2
	fi

	# don't exit in interactive mode
	[[ $- != *i* ]] && exit 1
}

if [[ ! -f "scripts/shared.sh" ]]; then
	die "Script must be run from the project's root directory"
fi

MKTEMP_CMDLINE="-t fooXXXXXX"

mktempf() {
	mktemp $MKTEMP_CMDLINE
}

mktempd() {
	mktemp -d $MKTEMP_CMDLINE
}

# Hack to prevent expansion of paths like /foo to
# C:\Program Files\Git\foo when using Git Bash under
# M$ Windows
adb-path() 
{
	[[ $# -eq 1 ]] || die "adb-path: no argument"
	
	echo -n "/.."

	if [[ ${1:0:1} != "/" ]]; then
		echo -n "/"
	fi

	echo $1
}

# For some obscure reason, adb shell always returns 0, regardless
# of the executed command's exit status. Hence this dirty hack.
adb-shell() {
	[[ $# -eq 0 ]] && die "adb-shell: no arguments"

	local tmp="${DTEMP}/.exitstatus"
	adb shell "$@; echo $? > $tmp"
	local status=$(adb shell cat $tmp | tr -d "\r\n")
	let status=$status+0

	adb shell rm $tmp

	return $status
}

run() {
	[[ $DEBUG -eq 1 ]] && echo $@
	"$@" || die "$1 exited with status $?"
}

require-grep-P()
{
	grep -q -P foobar <<< "foobar" &> /dev/null || die "Error: Script requires working grep -P"
}

#grep -q -P foobar <<< foobar || die "Error: grep -P does not work"
set -u

