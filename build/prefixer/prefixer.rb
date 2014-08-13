require 'rubygems'

source_files = Dir.glob(File.join("../../**", "*.java"))
new_prefix = File.open("new_prefix.txt", "r").read
old_prefix = File.open("old_prefix.txt", "r").read


def starts_with?(string, prefix)
  prefix = prefix.to_s
  string[0, prefix.length] == prefix
end


source_files.each do |source_file| 
  source_file_contents = File.open(source_file, "r").read
  if source_file_contents.start_with? old_prefix
    source_file_contents = source_file_contents.gsub(old_prefix,"")
  end
  unless source_file_contents.start_with? new_prefix
    source_file_contents = new_prefix + "\n" + source_file_contents  
    File.open( source_file, 'w' ) { | file | file.puts source_file_contents }
  end 
end

puts source_files
puts new_prefix
