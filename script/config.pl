use strict;
#use JSON;

# config file
my $configFile = "../exp/parameter/config.json";
my $in = openReader($configFile);
my $content = join("", <$in>);
close($in);
our $config;
#our $config = decode_json $content;
our $fws = "fws";
our $trec_eval = "trec_eval";

sub openReader {
	my $filename  = shift;
	open my $in, "<", $filename or die "cannot open $filename";
	return $in;
}

sub openWriter {
	my $filename  = shift;
	open my $out, ">", $filename or die "cannot open $filename";
	return $out;
}

sub loadQueryIDs {
	my $filename = shift;
	open my $file, $filename or die "cannot open $filename";
	my @queries = (); 
	while (my $line = <$file>) {
		chomp $line;
		my ($qid, $query) = split /\t/, $line;
		push @queries, $qid;
	}   
	return @queries;
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

sub loadQrelForSubtopic {
	my $file = shift;
	my $in = openReader($file);
	my %sqrels = ();
	while(my $line = <$in>) {
		chomp $line;
		my ($qidSid, $zero, $did, $rating) = split /\s+/, $line;
		my ($qid, $sid) = split /-/, $qidSid;
		$sqrels{$qid}->{$sid}->{$did} = $rating; 
	}
	close($in);
	return \%sqrels;
}


sub infoProcessing {
	my $name = shift;
	print STDERR "processing $name\n";
}

sub infoWritten {
	my $name = shift;
	print STDERR "written in $name\n";
}

sub call {
	my ($cmd) = @_;
	print STDERR $cmd."\n";
	return system($cmd) == 0;
}

sub callPrint {
	my ($cmd) = @_;
	print STDERR $cmd."\n";
	return 1;
}
1;
