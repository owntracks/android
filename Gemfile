source "https://gem.coop"

gem "fastlane", "~> 2.230.0"

# Ruby 3.4+ compatibility
gem "abbrev", "~> 0.1"
gem "ostruct", "~> 0.6"

gem "openssl", "~> 4.0.0"

plugins_path = File.join(File.dirname(__FILE__), 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)
