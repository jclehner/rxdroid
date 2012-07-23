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

