# qpRunFile
use strict;
my $outfile = "../exp/qp/run.id";

open my $out, ">$outfile" or die "cannot open $outfile";
my $id = 0;
my @models = qw/gmi gmj/;
my @gmRuns = qw/gm-ll-sample gm-prf_b1_ga-allcase gm-prf_b05_ga-allcase gm-prf_a2_ga-allcase/;

my @metrics = qw/6 10 15 19 24/;

for my $metric (@metrics) {
	for my $gmRun (@gmRuns) {
		for my $model (@models) {
			my $qpRunId = "$model-$metric-$gmRun";
			my $facetRunId = $gmRun;
			my $facetTuneId = $model =~ /^gm/ ? "$gmRun-f10" : "allmodel-f10";
			my $modelParams = $metric;
			$modelParams = "sum" if ($model eq "gmj");
			$modelParams = "sum-$metric" if ($model eq "gmi");
			print $out "$qpRunId\t$facetRunId\t$facetTuneId\t$model\t$modelParams\t$metric\n";
		}
	}
}


print "written in $outfile\n";
close $out;

#String qpRunId = elems[0];
#String facetRunId = elems[1]; // for gm directory
#String facetTuneId = elems[2];
#String model = elems[3];
#String modelParams = elems[4];
#int metricIdx = Integer.parseInt(elems[5]);

