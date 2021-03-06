use strict;
require 'config.pl';
my $expDir = "../exp";
my $dir = $expDir; 
my $facet = "facet";
my $facetTune = "facet-tune";
my $folds = $ARGV[0];
#!!! this is a very dangrous action,
#!!! make sure the code is correct before running it

migrateGmj();
migrateQdLdaPlsa();
migrateGmi($folds);

sub migrateQdLdaPlsa {
	my @models = qw/lda plsa qd/;
	for my $model (@models) {
		migrate($model);
	}
}

sub migrateGmj {
	my $model = "gmj";
	callPrint("mkdir -p $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/eval $dir/$facetTune/$model") or die;
}

sub migrateGmi {
	my $folds = shift;
	my $model = "gmi";
	callPrint("mkdir -p $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/eval $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/facet $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/cluster $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/params $dir/$facetTune/$model") or die;

	for my $fold (1..$folds) {
		callPrint("mkdir -p $dir/$facet/$model/$fold") or die;
		my $predictDir = "$dir/$facet/$model/$fold/predict"; 
		my $clusterDir = "$dir/$facet/$model/$fold/cluster";
		my $facetDir = "$dir/$facet/$model/$fold/facet";
		callPrint("mkdir -p $predictDir") or die;
		callPrint("mkdir -p $clusterDir") or die;
		callPrint("mkdir -p $facetDir") or die;

		for my $qid (loadQueryIDs("$dir/$facet/gm/train/$fold/train.query")) {

			callPrint("mkdir -p $facetDir/$qid") or die;
			callPrint("mkdir -p $predictDir/$qid") or die;
			callPrint("mkdir -p $clusterDir/$qid") or die;
			callPrint("mv $dir/$facet/gm/train/$fold/tune/$qid/*.cluster $clusterDir/$qid") or die;
			callPrint("mv $dir/$facet/gm/train/$fold/tune/$qid/*.facet $facetDir/$qid") or die;
			callPrint("mv $dir/$facet/gm/train/$fold/tune/$qid/*.p.data.gz* $predictDir/$qid") or die;
			callPrint("mv $dir/$facet/gm/train/$fold/tune/$qid/*.p.predict.gz* $predictDir/$qid") or die;
			callPrint("mv $dir/$facet/gm/train/$fold/tune/$qid/*.t.predict* $predictDir/$qid") or die;
		}
		callPrint("rm -r $dir/$facet/gm/train/$fold/tune") or die;

		callPrint("mkdir -p $dir/$facetTune/$model/tune/$fold") or die;
		callPrint("mv $dir/$facet/gm/train/$fold/eval $dir/$facetTune/$model/tune/$fold") or die;
	}
}

sub migrate {
	my $model = shift;

	callPrint("mkdir -p $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/eval $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/tune $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/facet $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/params $dir/$facetTune/$model") or die;
	callPrint("mv $dir/$facet/$model/run/* $dir/$facet/$model") or die;
	callPrint("rm -r $dir/$facet/$model/run") or die;
}


