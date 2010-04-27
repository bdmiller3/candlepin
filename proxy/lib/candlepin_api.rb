require 'base64'
require 'openssl'
require 'rubygems'
require 'rest_client'
require 'json'

class Candlepin

    attr_reader :identity_certificate, :consumer

    def initialize(host='localhost', port=8443)
        @base_url = "https://#{host}:#{port}/candlepin"
    end

    def use_credentials(username=nil, password=nil)
        create_basic_client(username, password)
    end

    def upload_satellite_certificate(certificate)
        post('/certificates', Base64.encode64(certificate).gsub(/\n/, ""))
    end

    def register(consumer, username=nil, password=nil)
        # TODO:  Maybe this should be created earlier?
        use_credentials(username, password)

        @consumer = post('/consumers', consumer)['consumer']

        identity_cert = @consumer['idCert']['cert']
        identity_key = @consumer['idCert']['key']
        @identity_certificate = OpenSSL::X509::Certificate.new(identity_cert)
        @identity_key = OpenSSL::PKey::RSA.new(identity_key)

        create_ssl_client
    end

    def get_owners
        get('/owners')
    end

    def get_owner(owner_id)
        get("/owners/#{owner_id}")
    end

    def create_owner(owner_name)
        owner = {
          'owner' => {
            'key' => owner_name,
            'displayName' => owner_name
          }
        }

        post('/owners', owner)
    end

    def get_consumer_types
        get('/consumertypes')
    end

    def get_consumer_type(type_id)
        get("/consumertypes/#{type_id}")
    end

    def create_consumer_type(type_label)
        consumer_type =  {
          'consumertype' => {
            'label' => type_label
          }
        }

        post('/consumertypes', consumer_type)
    end

    def delete_consumer_type(type_id)
        delete("/consumertypes/#{type_id}")
    end
    
    def get_pool(poolid)
      get("/pools/#{poolid}'")
    end

    def get_pools(params = {})
      path = "/pools?"
      path << "consumer=#{params[:consumer]}&" if params[:consumer]
      path << "owner=#{params[:owner]}&" if params[:owner]
      path << "product=#{params[:product]}&" if params[:product]
      return get(path)
    end

    def get_certificates()
        path = "/consumers/#{@consumer['uuid']}/certificates"
        return get(path)
    end

    def get_entitlement(entitlement_id)
        get("/entitlements/#{entitlement_id}")
    end

    def unregister(uuid = nil)
        uuid = @consumer['uuid'] unless uuid
        delete("/consumers/#{uuid}")
    end

    def revoke_all_entitlements()
        delete("/consumers/#{@consumer['uuid']}/entitlements")
    end

    def consume_product(product)
        post("/consumers/#{@consumer['uuid']}/entitlements?product=#{product}")
    end
    
    def list_products
      get("/products")
    end
    
    def create_product(product_json)
      return post("/products", product_json)
    end
    
    def consume_pool(pool)
        post("/consumers/#{@consumer['uuid']}/entitlements?pool=#{pool}")
    end

    def list_entitlements(product_id = nil)
        path = "/consumers/#{@consumer['uuid']}/entitlements"
        path << "?product=#{product_id}" if product_id
        get(path)
    end

    def list_rules()
        get_text("/rules")
    end

    def upload_rules(rule_set)
        post_text("/rules/", rule_set)
    end

    def get_consumer(cid)
        get("/consumers/#{cid}")
    end

    def unbind_entitlement(eid)
        delete("/consumers/#{@consumer['uuid']}/entitlements/#{eid}")
    end

    def get_subscriptions
        return get("/subscriptions")
    end

    def create_subscription(data)
        return post("/subscriptions", data)
    end

    def delete_subscription(subscription)
        return delete("/subscriptions/#{subscription}")
    end

    def get_subscription_tokens
        return get("/subscriptiontokens")
    end

    def create_subscription_token(data)
        return post("/subscriptiontokens", data)
    end

    def delete_subscription_token(subscription)
        return delete("/subscriptiontokens/#{subscription}")
    end

    def get_certificates(serials = [])
        path = "/consumers/#{@consumer['uuid']}/certificates"
        path += "?serials=" + serials.join(",") if serials.length > 0
        return get(path)
    end

    def get_certificate_serials
        return get("/consumers/#{@consumer['uuid']}/certificates/serials")
    end

    private

    def create_basic_client(username=nil, password=nil)
        @client = RestClient::Resource.new(@base_url, 
            username, password)
    end

    def create_ssl_client
        @client = RestClient::Resource.new(@base_url,
            :ssl_client_cert => @identity_certificate, 
            :ssl_client_key => @identity_key)
    end

    def get(uri)
        response = @client[uri].get :accept => :json

        return JSON.parse(response.body)
    end

    def get_text(uri)
      response = @client[uri].get
      return (response.body)
    end

    def post(uri, data=nil)
        data = data.to_json if not data.nil?
        response = @client[uri].post(data, :content_type => :json, :accept => :json)

        return JSON.parse(response.body)
    end
    
    def post_text(uri, data=nil)
        response = @client[uri].post(data, :content_type => 'text/plain', :accept => 'text/plain' )
        return response.body
    end

    def delete(uri)
        @client[uri].delete
    end
end

