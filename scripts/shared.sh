#!/bin/bash

PKG="at.caspase.rxdroid"

die() {
	if [[ $# -ne 0 ]]; then
		echo $* >&2
	else
		echo "error" >&2
	fi

	exit 1
}

mktemp() {
	command mktemp -t fooXXXXXX
}

mktempd() {
	command mktemp -d -t fooXXXXXX
}

run() {
	command -v $0 &> /dev/null || die "No such command: $0"
}
