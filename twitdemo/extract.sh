#!/bin/bash

ls data | xargs -I{} -P80 sh -c 'sed "s/^[[:digit:]]\+ //" data/{} | jq -r ". | select(.lang==\"es\") | [.id, .text, .retweetedStatus.text] | @tsv" | awk -F$"\t" "{OFS=\"\t\";if(length(\$2)>length(\$3)){tp=\$2}else{tp=\$3}print \$1,tp}" | sed -e "s/\\tRT /\\t/" -e "s/@[[:alnum:]]\\+//g" -e "s|https\\?://[[:graph:]]\\+||g" -e "s/\\\\[[:alpha:]]/ /g" 2>/dev/null' | grep '^[[:digit:]]\{6,\}' | sort -u -t$'\t' -k1,1 | tr -d \'\"\$\\ | tr -d "\"" | iconv -f UTF-8 -t ASCII//TRANSLIT  | tr A-Z a-z | tr -c "a-z0-9\n#" ' ' | sed 's/ \+/ /g' | tee twits.txt | sed -e 's/#[[:alnum:]]\+//g' -e 's/ \+/ /g' | tr -d '#0-9' | cut -f2- | tr ' ' '\n' | sed '/^\s*$/d' | awk '{cnt[$1]=++cnt[$1]}END{for(w in cnt){if(cnt[w]>=5)print w"\t"cnt[w]}}' | sort -n -k2,2 > twits.dict

mkdir tmp
split -nl/80 twits.txt tmp/p

find tmp -name 'p*' -printf '%f\n' | xargs -n1 -P80 -I{} sh -c 'sed "s/\$/ X/" tmp/{} | tr " " "\n" | sed -e "s/ \\+/ /g" -e "/^\$/d" | python3 wordseg/wordseg/core.py twits.dict | awk "(\$1==\"X\"){print buf;buf=\"\"}(\$1!=\"X\"){buf=buf\$0\" \"}" > tmp/x{}'

cat tmp/x* > twits-clean.txt

rm -Rf tmp
