#!/bin/bash

for f in ic_menu_*{_dark,_light}.png; do
	new=$(sed -e "s:_dark:_black:" <<< "$f")

	if [[ "$new" != "$f" ]]; then
		mv $f $new
	fi

	new=$(sed -e "s:_light:_white:" <<< "$f")

	if [[ "$new" != "$f" ]]; then
		mv $f $new
	fi
done
