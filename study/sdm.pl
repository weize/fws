#!/usr/bin/perl
use strict;

if (scalar @ARGV < 3) {
	print "usage: <query> <outname-prefix> [ch|en|none]\n";
	exit;
}
my $fch =  $ARGV[0]; # INPUT: chinese query 
my $fo  = $ARGV[1] . "-sdm"; # OUTPUT: query file
my $fp  = $ARGV[1] . "-sdm.param"; # OUTPUT: query param file
my $lan = $ARGV[2];

my $field;
if ($lan eq "ch") {
	$field = ".(sentence)";
} elsif ($lan eq "en") {
	$field = ".(xlate)";
} elsif ($lan eq "none") {
	$field = "";
}

open CN, "<:utf8", $fch or die;
chomp(my @chQ = <CN>);
close CN;
binmode(STDOUT, ":utf8");
open O, ">:utf8", $fo or die;
open P, ">:utf8", $fp or die;

print P "<parameters>\n";
for my $i (0..$#chQ) {
	my ($idc, $qc) = split /\t/, $chQ[$i];
	
	$qc = tokenize($qc);
	my $sdm = SDMQuery($qc);
	print O "$idc\t$sdm\n";	
	print P  QParam($idc, $sdm);	
}
print P "</parameters>\n";
close O;
close P;

sub SDMQuery {

	my $qc = shift;
	my @tokens = split /\s+/, $qc;
	my $entire = "";
	my $order = "";
	my $unorder = "";
	for my $i (0..$#tokens) {
		my $t = $tokens[$i];
		$entire .= $t . "$field ";
		last if ($i >= $#tokens);
		my $t2 = $tokens[$i] . " " . $tokens[$i+1];
		$order .= " #1($t2)$field ";
		$unorder .= " #uw8($t2)$field ";
	}

	my $sdm = "#weight( 0.85 #combine(" . $entire .  ") 0.1 #combine(" . $order . ")  0.05 #combine(" . $unorder . "))";
	return $sdm;
}

sub QParam {
	my ($qid, $query) = @_;
	return "<query>\n\t<type>indri</type>\n\t<number>". $qid ."</number>\n\t<text>\n\t\t" . $query . "\n\t</text>\n</query>\n";
}

sub tokenize {
	my $s = shift;
	$s = lc $s;
	$s =~ s/\p{Punct}/ /g;
	$s =~ s/([^0-9a-z ])/ \1 /g;
	$s =~ s/\s+/ /g;
	$s =~ s/(^\s+|\s+$)//g;
	return $s;
}
