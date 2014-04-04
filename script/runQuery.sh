galago=/mnt/nfs/work1/wkong/local/bin/galago
qsub -cwd -b y -o ../exp/query/query-sdm-rank -e ../exp/query/query-sdm-rank.err -l mem_free=4G -l mem_token=4G $galago batch-search ../exp/query/query-sdm.json
