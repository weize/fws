input=$1
output=$2
galago=/mnt/nfs/work1/wkong/local/bin/galago
echo qsub -cwd -b y -o $output -e $output.err -l mem_free=4G -l mem_token=4G $galago batch-search $input 
qsub -cwd -b y -o $output -e $output.err -l mem_free=4G -l mem_token=4G $galago batch-search $input 
#qsub -cwd -b y -o ../exp/query/query-sdm-rank -e ../exp/query/query-sdm-rank.err -l mem_free=4G -l mem_token=4G $galago batch-search ../exp/query/query-sdm.json
#qsub -cwd -b y -o ../data/doc-name/query-200-sdm-rank -e ../data/doc-name/query-200-sdm-rank.err -l mem_free=4G -l mem_token=4G $galago batch-search ../data/doc-name/query-200-sdm.json
