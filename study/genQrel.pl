use strict;

my $qidsRef = loadQids();


my $orgQrel = "qrels.diversity";
my $newQrel = "qrels";
open my $in, $orgQrel or die "cannot open $orgQrel";
open my $out, ">$newQrel" or die "cannot open $newQrel";
while(my $line = <$in>) {
	# 1 0 clueweb09-en0000-15-04138 0
	chomp $line;
	my ($qid, $sid, $docid, $rating) = split /\s+/,$line;
	if($sid and  exists $qidsRef->{$qid}) {
		print $out "$qid-$sid 0 $docid $rating\n";
	}
}
close $in;
close $out;

sub loadQids {
	my %qids= ();
	open my $in, "term-annotation" or die "cannot open term-annotation";
	while(my $line = <$in>) {
		chomp $line;
		my ($qid, $sid, $term) = split /\t/, $line; 
		$qids{$qid} = 1;

	}
	close $in;
	return \%qids;
}

