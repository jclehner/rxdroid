#!/bin/bash

readonly PKG="at.caspase.rxdroid"
readonly DTEMP="/sdcard/"

die() {
	if [[ $# -ne 0 ]]; then
		echo $* >&2
	else
		echo "Error" >&2
	fi

	# don't exit in interactive mode
	[[ $- != *i* ]] && exit 1
}

MKTEMP_CMDLINE="-t fooXXXXXX"

mktempf() {
	mktemp $MKTEMP_CMDLINE
}

mktempd() {
	mktemp -d $MKTEMP_CMDLINE
}

# For some obscure reason, adb shell always returns 0, regardless
# of the executed command's exit status. Hence this dirty hack.
adb-shell() {
	[[ $# -eq 0 ]] || die "adb-shell: no arguments"

	local tmp="${DTEMP}/.exitstatus"
	adb shell "$*; echo $? > $tmp"
	local status=$(adb shell cat $tmp | tr -d "\r\n")
	let status=$status+0

	adb shell rm -f $tmp

	return $status
}

run() {
	$* || die "$1 exited with status $?"
}

grep -q -P foobar <<< foobar || die "Error: grep -P does not work"
