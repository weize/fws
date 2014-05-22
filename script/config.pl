use strict;
use JSON;

# config file
my $configFile = "../exp/parameter/config.json";
my $in = openReader($configFile);
my $content = join("", <$in>);
close($in);
our $config = decode_json $content;

sub openReader {
	my $filename  = shift;
	open my $in, "<", $filename or die "cannot open $filename";
	return $in;
}

sub openWrite {
	my $filename  = shift;
	open my $out, ">", $filename or die "cannot open $filename";
	return $out;
}

sub loadSqrel {
	my $file = shift;
	my $in = openReader($file);
	my %sqrels = ();
	while(my $line = <$in>) {
		chomp $line;
		my ($qid, $sid, $did, $rating) = split /\s+/, $line;
		$sqrels{$qid}->{$sid}->{$did} = $rating; 
	}
	close($in);
	return \%sqrels;
}

1;
