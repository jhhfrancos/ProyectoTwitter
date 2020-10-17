sudo yum install -y cmake3 devtoolset-8

scl enable devtoolset-8 bash

wget https://dl.bintray.com/boostorg/release/1.67.0/source/boost_1_67_0.tar.gz

mkdir b167

cd b167

tar xf ../boost_1_67_0.tar.gz

./bootstrap.sh --prefix=/opt/boost

sudo ./b2 -j8 install --prefix=/opt/boost --with=all

git clone https://github.com/microsoft/SPTAG.git

cd SPTAG

mkdir build

cd build

cmake3 ../ -DBOOST_ROOT=/opt/boost

make -j8


TF counts to tfidf and SPTAG

prereqs: datamash, R dataset: NIPS Conference Papers 1987-2015 Data Set https://archive.ics.uci.edu/ml/datasets/NIPS+Conference+Papers+1987-2015


cat NIPS_1987-2015.csv | tr ',' '\t' > NIPS_1987-2015.tsv
datamash transpose < NIPS_1987-2015.tsv > trnips.tsv
read.table(file = 'trnips.tsv', sep = '\t', row.names=1,header = TRUE)
tf<-apply(f,1,function(x){0.5+0.5*x/max(x)})
idf<-apply(f,2,function(x){log(as.numeric(dim(f)[[1]])/length(which(x>0)))})
tfidf<-apply(tf,2,function(x){x*idf})
tfidf[is.na(tfidf)]<-0
write.table(t(tfidf),file="nips-tfidf.tsv",quote=FALSE,sep='\t')
cat nips-tfidf.tsv| sed 1d  | awk '{printf $1"\t";for(i=2;i<=NF;i++){printf $(i)"|"};print ""}' > nips.sptag

#extract tweets test
ls data | xargs -I{} -P80 sh -c 'sed "s/^[[:digit:]]\+ //" data/{} | jq -r ". | select(.lang==\"es\") | [.id, .text, .retweetedStatus.text] | @tsv" | awk -F$"\t" "{OFS=\"\t\";if(length(\$2)>length(\$3)){tp=\$2}else{tp=\$3}print \$1,tp}" | sed -e "s/\\tRT /\\t/" -e "s/@[[:alnum:]]\\+//g" -e "s|https\\?://[[:graph:]]\\+||g" -e "s/\\\\[[:alpha:]]/ /g" 2>/dev/null' | grep '^[[:digit:]]\{6,\}' | sort -u -t$'\t' -k1,1 | tr -d \'\"\$\\ > twits.txt


#postprocessing
cut -d$'\t' -f2 ../simple-tfidf/twit1/clean.txt | tr ' ' '\n' | sed '/^\s*$/d' | awk '{cnt[$1]=++cnt[$1]}END{for(w in cnt){if(cnt[w]>=5)print w"\t"cnt[w]}}' | sort -n -k2,2 > twits.dict
