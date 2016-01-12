use strict;
#my $i= $ARGV[0];
my @metrics;

push @metrics, 6..24;
push @metrics, 31..49;

my @names = qw/tP tR tF poP poR poF oPRFa1 oPRFa2 oPRFa3 oPRFa4 oPRFa5 oPRFa6 oPRFa7 oPRFa8 oPRFa9 oPRFa10 oPRFaf2 oPRFaf3 oPRFaf4 oPRFaf5 oPRFaf6 oPRFaf7 oPRFaf8 oPRFaf9 oPRFaf10 tPtr tRtr tFtr poPtr poRtr poFtr oPRFa1tr oPRFa2tr oPRFa3tr oPRFa4tr oPRFa5tr oPRFa6tr oPRFa7tr oPRFa8tr oPRFa9tr oPRFa10tr oPRFtraf2 oPRFtraf3 oPRFtraf4 oPRFtraf5 oPRFtraf6 oPRFtraf7 oPRFtraf8 oPRFtraf9 oPRFtraf10 tSizeA tSizeS tSizeS2 pcSizeA poSizeA pSizeS pSizeS2/;


#push @metrics, qw/4 5 18 29 40 51/; # classification
#push @metrics, qw/6 7 21 32 43 54 24 35 46 57/; #clustering
#push @metrics, qw/8 9 25 36 47 58 26 37 48 59/; # prf
#push @metrics, qw/11 12 13/; # prf

#my @names = qw/ P wP R wR F wF Fc wFc PRF wPRF NDCG pNDCG prNDCG fNDCG purity NMI tP tR tF pcP pcR pcF poP poR poF cPRF oPRF tPtr tRtr tFtr pcPtr pcRtr pcFtr poPtr poRtr poFtr cPRFtr oPRFtr tPfe tRfe tFfe pcPfe pcRfe pcFfe poPfe poRfe poFfe cPRFfe oPRFfe tPfr tRfr tFfr pcPfr pcRfr pcFfr poPfr poRfr poFfr cPRFfr oPRFfr tSizeA tSizeS tSizeS2 pcSizeA poSizeA pSizeS pSizeS2/;

my $rank = $ARGV[0];
my $run = $ARGV[1];

die "topFacetNum=?" if !$rank;
die "run=?" if !$run;
#header
print "run\tmodel\ttuneMetric\tscore\ttSize\ttP\ttR\ttF\tpP\tpR\tpF\ttPtr\ttRtr\ttFtr\tpPtr\tpRtr\tpFtr\n";
for my $i (@metrics) {
	my $name  = $names[$i];
	my $file;
	my @res;

	# ============ gmi
	$file = "gmi/eval/gmi.sum-$i.$rank.eval";
	@res = grepTunedResults($file, $i);
	print "$run\tgmIndep\t$name\t".join("\t", @res)."\n";
	# ============ gmj
	$file = "gmj/eval/gmj.sum.$rank.eval";
	my @res = grepTunedResults($file, $i);
	print "$run\tgmJoint\t$name\t".join("\t", @res)."\n";
}

sub grepTunedResults {
	my ($file, $tuneIdx) = @_;
	my @res;	
	open my $in, $file or die;
	while(my $line = <$in>) {
		chomp $line;
		#qid eval0 eval1 ...	
		#qid tP tR tF pP pR pF eval6 eval7 
		my @elems = split /\t/, $line;
		next if ($elems[0] ne "all");
		push @res, $elems[$tuneIdx + 1];	
		push @res, $elems[53];	
		push @res, @elems[1..6];	
		push @res, @elems[26..31];	
	}
	close $in;
	return @res;
}

