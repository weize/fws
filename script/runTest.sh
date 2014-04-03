fws=/mnt/nfs/work1/wkong/local/bin/fws
qsub -cwd -b y -o tmp -e tmp.err -l mem_free=4G -l mem_token=4G $fws test ../exp/config.json
