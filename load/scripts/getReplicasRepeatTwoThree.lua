counter = 0
repeatLimit = 1000
wrk.method = "GET"

request = function()
    local path = "/v0/entity?id=" .. counter .. "&replicas=2/3"
    counter = counter + 1
    counter = counter % repeatLimit
    return wrk.format(nil, path)
end
