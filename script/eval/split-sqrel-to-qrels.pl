# splits sqrel to qrels by subtopics.
# Resulting files <qid>-<sid>.qrel contains relevance judgements for each subtopics
use strict;
require 'config.pl';

our $config;

my $sqrelFile = $config->{"sqrel"};
my $sqrelDir =  $config->{"sqrelDir"};
infoProcessing($sqrelFile);

my $sqrels = loadSqrel($sqrelFile);

for my $qid (keys %{$sqrels}) {
for my $sid (keys %{$sqrels->{$qid}}) {
	next if ($sid eq "0");
	my $qrelFile = "$sqrelDir/$qid-$sid.qrel";
	my $writer = openWriter($qrelFile);
	for my $did (keys %{$sqrels->{$qid}->{$sid}}) {
		my $rating = $sqrels->{$qid}->{$sid}->{$did};
		print $writer "$qid 0 $did $rating\n";
	}
	close($writer);
	infoWritten($qrelFile);
}
}

