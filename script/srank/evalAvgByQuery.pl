use strict;
require 'config.pl';
my $infile = $ARGV[0]; 
my $outfile = $ARGV[1];

my $in = openReader($infile);
my $header;

while (my $line = <$in>) {
	chomp $line;
	if ($line =~ m/^#.+/) {
		$header = $lne;
		next;
	}
	my @elems = split /\t/, $line;
	my $qidSid = @elems[0];
	my ($qid, $sid) = split /-/, $line;
}
close $in;

