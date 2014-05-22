use strict;
require 'config.pl';

my $sqrelFile = "../data/qrel/all.diversity.sqrel";
my $qrelFile = "../data/qrel/all.adhoc.qrel";
my $sqrels = loadSqrel($sqrelFile);
my $qrels = loadSqrel($qrelFile);

# chekc if all nonrel in qrels is non rel in sqrels

for my $qid (keys %{$qrels}) {
	my $docs = $qrels->{$qid}->{"0"};
	my $sqrel = $sqrels->{$qid};
	for my $did (keys %{$docs}) {
		my $rating = $docs->{$did};
		if ($rating <= 0) {
			for my $sid (sort {$a <=>$b} keys %{$sqrel}) {
				if (exists $sqrel->{$sid}->{$did}) {
					my $rating2 = $sqrel->{$sid}->{$did};
					if ($rating != $rating2) {
						#					print "$qid\t$sid\t$did\t$rating\t$rating2\n";
					}
				}
			}
		}
	}
}


# check if any doc judged in qrels is also judged for sqrel

for my $qid (keys %{$qrels}) {
	# load docs for sqrel
	my $sqrel = $sqrels->{$qid};
	my %sdocs = ();
	for my $sid (keys %{$sqrel}) {
		for my $did (keys %{$sqrel->{$sid}}) {
			$sdocs{$did} = 1;
		}
	}

	my $docs = $qrels->{$qid}->{"0"};
	for my $did (keys %{$docs}) {
		if (!exists $sdocs{$did}) {
			print "$qid\t$did\n";
		}
	}
}
