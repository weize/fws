use strict;
#my $i= $ARGV[0];
my @metrics;

push @metrics, qw/4 5 18 29 40 51/; # classification
push @metrics, qw/6 7 21 24 32 35 43 46 54 57/; #clustering
push @metrics, qw/8 9 25 26 36 37 47 48 58 59/; # prf
push @metrics, qw/11 12 13/; # prf
for my $i (@metrics) {
	print "$i\n";
	my $cmd = "grep all */eval/*.$i.10.eval; grep all gmi/eval/*-$i.10.eval | grep -v avg; grep all gmj/eval/*.10.eval |grep -v avg";
	system($cmd);
}

