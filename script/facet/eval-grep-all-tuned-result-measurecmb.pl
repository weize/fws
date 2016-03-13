use strict;
#my $i= $ARGV[0];
my @metrics = qw/2 5 6 9 12 13 14 15 16 17 18 19/; #tune measures


my @names = qw/TP TR TF PP PR PF PRF1,1 wTP wTR wTF wPP wPR wPF wPRF1,1 NDCG pNDCG prNDCG fNDCG purity NMI tSizeAnn tSizeSys tSizeSysDup pcSizeAnn poSizeAnn pSizeSys pSizeSysDup/;
my @models = qw/gmi gmj qd lda plsa/;


#push @metrics, qw/4 5 18 29 40 51/; # classification
#push @metrics, qw/6 7 21 32 43 54 24 35 46 57/; #clustering
#push @metrics, qw/8 9 25 36 47 58 26 37 48 59/; # prf
#push @metrics, qw/11 12 13/; # prf

#my @names = qw/ P wP R wR F wF Fc wFc PRF wPRF NDCG pNDCG prNDCG fNDCG purity NMI tP tR tF pcP pcR pcF poP poR poF cPRF oPRF tPtr tRtr tFtr pcPtr pcRtr pcFtr poPtr poRtr poFtr cPRFtr oPRFtr tPfe tRfe tFfe pcPfe pcRfe pcFfe poPfe poRfe poFfe cPRFfe oPRFfe tPfr tRfr tFfr pcPfr pcRfr pcFfr poPfr poRfr poFfr cPRFfr oPRFfr tSizeA tSizeS tSizeS2 pcSizeA poSizeA pSizeS pSizeS2/;

my $run = $ARGV[0];
my $path = $ARGV[1];
$path = "./" if (!$path);
my $rank = 10;

die "run=?" if !$run;
#header
print "run\tmodel\ttuneMetric\t".join("\t", @names)."\n";
for my $i (@metrics) {
 	my $name  = $names[$i];

	for my $model (@models) {
		my $file = "$model/eval/$model.$i.$rank.eval";
		$file = "$model/eval/$model.sum-$i.$rank.eval" if ($model eq "gmi");
		$file = "$model/eval/$model.sum.$rank.eval" if ($model eq "gmj");
		$file = "$path/$file";
		next if (! -e $file);
		my @res = grepTunedResults($file);
		print "$run\t$model\t$name\t".join("\t", @res)."\n";
	}
}

sub grepTunedResults {
	my ($file) = @_;
	open my $in, $file or die;
	while(my $line = <$in>) {
		chomp $line;
		my @elems = split /\t/, $line;
		if ($elems[0] eq "all") {
			close $in;
			return @elems[1..$#elems];
		}
	}
	close $in;
}

