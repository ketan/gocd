##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

shared_examples_for :auto_refresh do
  before do
    class << @template
      include ApplicationHelper
    end
  end

  it "should include auto-refresh javascript if autoRefresh is true" do
    params[:autoRefresh] = "anything"
    render @partial
    response.should have_tag("script[type='text/javascript']", @ajax_refresher)
  end

  it "should include auto-refresh javascript if autoRefresh is absent" do
    render @partial
    response.should have_tag("script[type='text/javascript']", @ajax_refresher)
  end

  it "should not include auto-refresh javascript if autoRefresh is false" do
    params[:autoRefresh] = "false"
    render @partial
    response.should_not have_tag("script[type='text/javascript']", @ajax_refresher)
  end

end
