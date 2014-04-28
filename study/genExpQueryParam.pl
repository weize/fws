use strict;

my $termsRef = loadTerms(); 
my $queriesRef = loadQueries();

my $queryParam = "query-exp.json";
open my $out, ">$queryParam" or die "cannot open $queryParam";
# generate query parameter file
print $out "{\n";
print $out "\"index\" : \"../data/index\",\n";
print $out "\"queries\" : [    {\n";

my @qidSids = sort{$termsRef->{$a}->[0] <=> $termsRef->{$b}->[0] || $termsRef->{$a}->[1] <=> $termsRef->{$b}->[1]} keys %{$termsRef};

# first
my $qidSid = $qidSids[0];
my ($qid, $sid, $termStr) = @{$termsRef->{$qidSid}};
my $query = $queriesRef->{$qid};

my $q = expandQuery($query, $termStr);
print $out "\"number\" : \"$qidSid\",\n"; 
print $out "\"test\" : \"$q\",\n"; 
print $out "}"; 

for my $qidSid (@qidSids[1..$#qidSids]) {
	my ($qid, $sid, $termStr) = @{$termsRef->{$qidSid}};
	my $query = $queriesRef->{$qid};

	my $q = expandQuery($query, $termStr);
	print $out ",    {\n"; 
	print $out "\"number\" : \"$qidSid\",\n"; 
	print $out "\"test\" : \"$q\",\n"; 
	print $out "}"; 
}

print $out "],\n"; 
print $out "\"requested\" : 1000\n"; 
print $out "}\n"; 
close $out;

sub expandQuery {
	my ($query, $termStr) = @_;
	return "test";
}
sub loadTerms {
	my %terms= (); 
	open my $in, "term-annotation" or die "cannot open term-annotation";
	while(my $line = <$in>) {
		chomp $line;
		my ($qid, $sid, $term) = split /\t/, $line; 
		$terms{"$qid-$sid"} = [$qid, $sid, $term];
	}
	close $in;
	return \%terms;
}

sub loadQueries {
	my %queries = (); 
	my $file = "query";
	open my $in, $file or die "cannot open $file";
	while(my $line = <$in>) {
		chomp $line;
		my ($qid, $query) = split /\t/, $line; 
		$queries{"$qid"} = $query;
	}
	close $in;
	return \%queries;
}
