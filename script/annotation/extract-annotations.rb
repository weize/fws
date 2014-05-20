#!/usr/bin/ruby
$:.unshift File.expand_path('.')
require 'json'
require 'annotation-backend.rb'

fileName = "../exp/annotation/from-db/facet-annotation.json"
db = AnnotationDB.new

annotations = db.getAllAnnotatedAolUsers()

file = File.open(fileName, "w")
annotations.each do |annotation|
 #   puts annotation.to_json #if db.annotationComplete?(annotation)
    file.write(annotation.to_json)
    file.write("\n");
end

file.close()
puts "Written in " + fileName
