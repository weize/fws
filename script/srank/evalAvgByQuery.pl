use strict;
require 'config.pl';
my $infile = $ARGV[0]; 
my $outfile = $ARGV[1];

my $in = openReader($infile);
my $out = openWriter($outfile);
my $header;

my %map; # qid -> sid ->[score1, score2, ...]
while (my $line = <$in>) {
	chomp $line;
	if ($line =~ m/^#.+/) {
		$header = $line;
		next;
	}
	my @elems = split /\t/, $line;
	my $qidSid = $elems[0];
	if ($qidSid eq "all") {
		next;
	}
	my ($qid, $sid) = split /-/, $line;
	
	
	my @scores = @elems[1..$#elems];
	$map{$qid}->{$sid} = \@scores;
}
close $in;

print $out "$header\n";
my @avg;
for my $qid(keys %map) {
	my $ref = $map{$qid};

	# metric size
	my $metricSize;
	for my $sid (keys %{$ref}) {
		$metricSize = scalar @{$ref->{$sid}};
		last;
	}
	my @scores = (); 
	for my $i (0..($metricSize-1)) {
		$scores[$i] = 0;
	}

	my $size = 0;
	for my $sid (keys %{$ref}) {
		$size ++;
		for my $i (0..($metricSize-1)) {
			$scores[$i] += $ref->{$sid}->[$i];
		}
	}

	for my $i (0..($metricSize-1)) {
		$scores[$i] /= $size;
	}
	
	my @scoreStrs;
	my $i = 0;
	for my $score (@scores) {
		push @scoreStrs, sprintf("%.4f", $score);
		$avg[$i] += $score;
		$i ++;
	}
	print $out $qid."\t".join("\t", @scoreStrs)."\n";

}


my $querySize = scalar keys %map;
for my $i (0..$#avg) {
	$avg[$i] /= $querySize;
}

my @scoreStrs;
	my $i = 0;
	for my $score (@avg) {
		push @scoreStrs, sprintf("%.4f", $score);
		$avg[$i] += $score;
		$i ++;
	}
	print $out "all\t".join("\t", @scoreStrs)."\n";
close $out;