use strict;

my $qid = $ARGV[0];
my $rank = $ARGV[1];
my $type = $ARGV[2];

my $rankFile = "../exp/query/query-sdm-rank";

my $docs = loadDocuments($rankFile);
my $fileName = $docs->{"$qid:$rank"};
my $path = $type eq "doc" ? "../exp/doc/$qid/$fileName.html" : "../exp/parse/$qid/$fileName.parse";
print "$path\n";


sub loadDocuments {
	my ($file) = @_;
	open my $in, $file or die "cannot open $file";
	my %docs = ();
	while (my $line = <$in>) {
		chomp $line;
		my ($qid, $Q0, $docName, $rank, $score, $runID) = split /\s+/, $line;
		$docs{"$qid:$rank"} = $docName;
	}
	close $in;
	return \%docs;
}


