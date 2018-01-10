counter = 0
wrk.method = "GET"

request = function()
    local path = "/v0/entity?id=" .. counter .. "&replicas=2/3"
    counter = counter + 1
    return wrk.format(nil, path)
end
