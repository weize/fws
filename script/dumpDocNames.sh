# dump document names from galago index
fws=/mnt/nfs/work1/wkong/local/bin/fws
index=../data/index

qsub -cwd -b y -o ../data/doc-name/doc-name -e ../data/doc-name.err -l mem_free=4G -l mem_token=4G $fws dump-name --index=$index
