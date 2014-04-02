qsub -cwd -b y -o ../exp/query/result -e ../exp/query/result.err -l mem_free=4G -l mem_token=4G galago batch-search ../exp/query/query-sdm.json
