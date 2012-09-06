#!/usr/bin/perl
use strict;
use LWP::Simple;

sub getXrefByDbName {
    my ( $xrefs, $dbName ) = @_;
    for my $xref (split (/\|/, $xrefs)){
        my ($db, $id, $txt) = split /[:\(\)]/, $xref;
         if( $db eq $dbName ) {
            return $id;
         }
    }    
    return $xrefs;
}

sub readPsicquic {
    my ( $url, $query, $from, $size ) = @_;
    
    # Remove the trailing /psicquic at the end of the URL so we can build  a REST URL.
    my $finalUrl = $url =~ s/\/psicquic$//;
    my $queryUrl = $url . "/current/search/query/" . $query . "?firstResult=" . $from . "&maxResults=" . $size;
    
    my $content = get $queryUrl;
    die "Couldn't get $queryUrl" unless defined $content;

    # Now list all interactions
    my @lines = split(/\n/, $content);
    my $count = @lines;
    for my $line (@lines) {
      my @flds = split(/\t/, $line); # split tab delimited lines
      
      # split fields of a PSIMITAB 2.5 line
      my ($idA, $idB, $altIdA, $altIdB, $aliasA, $aliasB, $detMethod, $author, $pub, $orgA, $orgB, $intType, $sourceDb, $intId, $conf) = @flds;
      
      print getXrefByDbName($idA, "uniprotkb") . " interacts with " . getXrefByDbName($idB, "uniprotkb") . "\n";
    }
    
    return $count;
}

################################################################################
# Registry URL that lists all ACTIVE PSICQUIC services
# Documentation: http://code.google.com/p/psicquic/wiki/Registry
my $registryUrl = 'http://www.ebi.ac.uk/Tools/webservices/psicquic/registry/registry?action=ACTIVE&format=txt';

my ($miql) = @ARGV;
print "Searching for '$miql'...\n\n";

my $registryContent = get $registryUrl;
die "Couldn't get $registryUrl" unless defined $registryContent;

# Now list all services
my $totalCount = 0;
my @services = split(/\n/, $registryContent);
for my $service (@services){
  my @flds = split(/=/, $service);
  my ($serviceName, $serviceUrl) = @flds;
  print "$serviceName  --->  $serviceUrl\n";
  
    my $current = 0;
    my $lineCount;
    do {
          # Download a MITAB page (200 lines max per read)          
          $lineCount = readPsicquic $serviceUrl, $miql, $current, 200;
          $current = $current + $lineCount;
    } while( $lineCount == 200 );
    
    print "Total: " . $current . " interaction(s).\n\n";
    $totalCount = $totalCount + $current;
}

print "Retreived " . $totalCount . " binary interaction(s) across " . @services . " service(s)\n\n";